-- 리콜 제안은 물리 상태를 바꾸지 않고 안전 적격성만 차단한다.
CREATE TABLE recall_proposals (
 recall_proposal_id BIGSERIAL PRIMARY KEY,
 incident_lot_id BIGINT NOT NULL REFERENCES production_lots(production_lot_id),
 governance_action_id BIGINT NOT NULL UNIQUE REFERENCES governance_actions(governance_action_id),
 case_id BIGINT NOT NULL REFERENCES cases(case_id), work_item_id BIGINT NOT NULL,
 proposed_by_agent_id BIGINT REFERENCES agents(agent_id), proposed_by_user_id BIGINT REFERENCES users(user_id),
 version INT NOT NULL CHECK(version>0), proposal_hash CHAR(64) NOT NULL CHECK(proposal_hash ~ '^[0-9a-f]{64}$'),
 snapshot JSONB NOT NULL, reason TEXT NOT NULL CHECK(length(trim(reason))>0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 retain_until TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP + INTERVAL '2 years',
 FOREIGN KEY(work_item_id,case_id) REFERENCES work_items(work_item_id,case_id),
 CHECK(num_nonnulls(proposed_by_agent_id,proposed_by_user_id)=1),
 CHECK(retain_until>=created_at+INTERVAL '2 years'), UNIQUE(incident_lot_id,version)
);
CREATE TABLE recall_scope_lots (
 recall_proposal_id BIGINT NOT NULL REFERENCES recall_proposals(recall_proposal_id),
 lot_id BIGINT NOT NULL REFERENCES production_lots(production_lot_id),
 PRIMARY KEY(recall_proposal_id,lot_id)
);
ALTER TABLE recalls ADD COLUMN governance_action_id BIGINT REFERENCES governance_actions(governance_action_id),
 ADD COLUMN retain_until TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP + INTERVAL '2 years',
 ADD CONSTRAINT ck_recall_minimum_retention CHECK(retain_until>=created_at+INTERVAL '2 years');
CREATE UNIQUE INDEX uk_recall_action_lot ON recalls(governance_action_id,lot_id);
CREATE TABLE recall_reports (
 report_id BIGSERIAL PRIMARY KEY,
 governance_action_id BIGINT NOT NULL UNIQUE REFERENCES governance_actions(governance_action_id),
 status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status='PENDING'),
 transport TEXT NOT NULL DEFAULT 'OFFLINE' CHECK(transport='OFFLINE'),
 due_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 retain_until TIMESTAMPTZ NOT NULL, manifest JSONB NOT NULL CHECK(jsonb_typeof(manifest)='object'),
 submitted_at TIMESTAMPTZ CHECK(submitted_at IS NULL), confirmation TEXT CHECK(confirmation IS NULL),
 CHECK(retain_until>=created_at+INTERVAL '2 years'), CHECK(due_at<=created_at)
);
INSERT INTO users(name,email,password,role,is_active)
 VALUES ('Recall governance service','recall-service@mulino.internal','disabled-service-identity','VIEWER',false);
DO $$ DECLARE t TEXT; BEGIN
 FOREACH t IN ARRAY ARRAY['recall_proposals','recall_scope_lots','recall_reports','recalls'] LOOP
   EXECUTE format('CREATE TRIGGER trg_recall_immutable BEFORE UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_modification()',t);
   EXECUTE format('CREATE TRIGGER trg_recall_no_truncate BEFORE TRUNCATE ON %I FOR EACH STATEMENT EXECUTE FUNCTION prevent_audit_log_modification()',t);
 END LOOP;
 FOREACH t IN ARRAY ARRAY['recall_proposals','recall_scope_lots','production_lots','production_records','production_product_inputs','production_ingredients','outbound_lots','outbound','orders','order_items','customers','products','raw_materials','purchase_orders','purchase_order_items','warehouses'] LOOP
   EXECUTE format('CREATE TRIGGER trg_recall_source_lock BEFORE INSERT OR UPDATE OR DELETE ON %I FOR EACH STATEMENT EXECUTE FUNCTION lock_planning_data()',t);
 END LOOP;
END $$;
CREATE FUNCTION recall_lot_barrier(target BIGINT) RETURNS BOOLEAN AS $$
 WITH RECURSIVE blocked(lot_id) AS (
  SELECT lot_id FROM recall_scope_lots
  UNION SELECT p.lot_id FROM blocked b JOIN production_product_inputs i ON i.source_production_lot_id=b.lot_id JOIN production_records p USING(production_record_id)
 ) SELECT EXISTS(SELECT 1 FROM blocked WHERE lot_id=target);
$$ LANGUAGE sql STABLE;
CREATE FUNCTION recall_raw_barrier(target BIGINT) RETURNS BOOLEAN AS $$
 SELECT EXISTS(SELECT 1 FROM recall_proposals q, jsonb_array_elements(q.snapshot->'rawLots') r WHERE (r->>'raw_material_lot_id')::bigint=target AND (r->>'incidentRoot')::boolean);
$$ LANGUAGE sql STABLE;
CREATE FUNCTION guard_recall_consumption() RETURNS TRIGGER AS $$
DECLARE source_id BIGINT; target_id BIGINT;
BEGIN
 IF TG_TABLE_NAME='production_product_inputs' THEN
   source_id:=NEW.source_production_lot_id;
   SELECT lot_id INTO target_id FROM production_records WHERE production_record_id=NEW.production_record_id;
 ELSIF TG_TABLE_NAME='production_ingredients' THEN
   IF recall_raw_barrier(NEW.raw_material_lot_id) THEN RAISE EXCEPTION 'raw LOT has unresolved recall risk' USING ERRCODE='23514'; END IF;
   SELECT lot_id INTO target_id FROM production_records WHERE production_record_id=NEW.production_record_id;
 ELSE source_id:=NEW.lot_id; END IF;
 IF EXISTS(SELECT 1 FROM production_lots WHERE production_lot_id IN(source_id,target_id) AND status='RECALLED') OR recall_lot_barrier(source_id) OR recall_lot_barrier(target_id) THEN
   RAISE EXCEPTION 'product LOT has unresolved recall risk' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_recall_product_guard BEFORE INSERT OR UPDATE ON production_product_inputs FOR EACH ROW EXECUTE FUNCTION guard_recall_consumption();
CREATE TRIGGER trg_recall_raw_guard BEFORE INSERT OR UPDATE ON production_ingredients FOR EACH ROW EXECUTE FUNCTION guard_recall_consumption();
CREATE TRIGGER trg_recall_outbound_guard BEFORE INSERT OR UPDATE ON outbound_lots FOR EACH ROW EXECUTE FUNCTION guard_recall_consumption();
CREATE FUNCTION guard_recalled_status() RETURNS TRIGGER AS $$
BEGIN
 IF NEW.status='RECALLED' AND (TG_OP='INSERT' OR OLD.status<>'RECALLED') AND NOT EXISTS(
 SELECT 1 FROM recalls r JOIN governance_decisions d USING(governance_action_id) JOIN users u ON u.user_id=d.decided_by
 WHERE r.lot_id=NEW.production_lot_id AND d.decision='APPROVE' AND d.is_final AND u.role='ADMIN' AND u.is_active) THEN
 RAISE EXCEPTION 'RECALLED requires final ADMIN decision' USING ERRCODE='23514'; END IF;
 IF TG_OP='UPDATE' AND OLD.status='RECALLED' AND NEW.status<>'RECALLED' THEN RAISE EXCEPTION 'recalled LOT cannot be released' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_recalled_status BEFORE INSERT OR UPDATE ON production_lots FOR EACH ROW EXECUTE FUNCTION guard_recalled_status();
CREATE FUNCTION validate_recall_allocations() RETURNS TRIGGER AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM outbound o WHERE o.outbound_id IN (
 SELECT outbound_id FROM outbound_lots WHERE lot_id IN(SELECT lot_id FROM recall_scope_lots))
 AND o.quantity<>(SELECT coalesce(sum(lot_quantity),0) FROM outbound_lots l WHERE l.outbound_id=o.outbound_id)) THEN
 RAISE EXCEPTION 'affected outbound allocation is incomplete' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END;
$$ LANGUAGE plpgsql;
CREATE CONSTRAINT TRIGGER trg_recall_allocation_lots AFTER INSERT OR UPDATE OR DELETE ON outbound_lots DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION validate_recall_allocations();
CREATE CONSTRAINT TRIGGER trg_recall_allocation_outbound AFTER INSERT OR UPDATE OR DELETE ON outbound DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION validate_recall_allocations();
CREATE FUNCTION protect_recall_action() RETURNS TRIGGER AS $$
BEGIN
 IF OLD.resource_type='RECALL_PROPOSAL' THEN
 IF TG_OP='DELETE' OR (to_jsonb(OLD)-'status') IS DISTINCT FROM (to_jsonb(NEW)-'status') OR (NEW.status<>OLD.status AND NOT(OLD.status='PENDING' AND NEW.status IN('APPROVED','BLOCKED','CANCELLED','EXPIRED'))) THEN RAISE EXCEPTION 'invalid recall action mutation' USING ERRCODE='23514'; END IF;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF; RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_recall_action_guard BEFORE UPDATE OR DELETE ON governance_actions FOR EACH ROW EXECUTE FUNCTION protect_recall_action();
CREATE OR REPLACE FUNCTION validate_purchase_attention_scope() RETURNS TRIGGER AS $$
BEGIN
 IF NEW.governance_action_id IS NOT NULL AND NOT (
 EXISTS(SELECT 1 FROM governance_actions a WHERE a.governance_action_id=NEW.governance_action_id AND a.replenishment_plan_id IS NOT NULL AND a.case_id=NEW.case_id AND a.work_item_id=NEW.work_item_id)
 OR EXISTS(SELECT 1 FROM inbound_inspections q WHERE q.governance_action_id=NEW.governance_action_id AND q.case_id=NEW.case_id AND q.work_item_id=NEW.work_item_id)
 OR EXISTS(SELECT 1 FROM recall_proposals q WHERE q.governance_action_id=NEW.governance_action_id AND q.case_id=NEW.case_id AND q.work_item_id=NEW.work_item_id)) THEN
 RAISE EXCEPTION 'approval Attention must match its action Case and Work Item' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Existing invalid history remains visible to trace; new edges cannot introduce cycles.
CREATE FUNCTION production_lot_graph_acyclic() RETURNS BOOLEAN AS $$
 WITH RECURSIVE edges AS (
 SELECT i.source_production_lot_id source,r.lot_id target FROM production_product_inputs i JOIN production_records r USING(production_record_id)
 ), paths AS (
 SELECT source,target,ARRAY[source,target] path,source=target cycle FROM edges
 UNION ALL SELECT p.source,e.target,p.path||e.target,e.target=ANY(p.path) FROM paths p JOIN edges e ON e.source=p.target WHERE NOT p.cycle
 ) SELECT NOT EXISTS(SELECT 1 FROM paths WHERE cycle);
$$ LANGUAGE sql STABLE;
CREATE FUNCTION validate_recall_product_graph() RETURNS TRIGGER AS $$
BEGIN
 IF NOT production_lot_graph_acyclic() THEN RAISE EXCEPTION 'production LOT graph has a cycle' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_recall_product_graph AFTER INSERT OR UPDATE ON production_product_inputs FOR EACH STATEMENT EXECUTE FUNCTION validate_recall_product_graph();
CREATE TRIGGER trg_recall_record_graph AFTER UPDATE OF lot_id ON production_records FOR EACH STATEMENT EXECUTE FUNCTION validate_recall_product_graph();

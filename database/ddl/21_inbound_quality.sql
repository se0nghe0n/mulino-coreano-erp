-- 입고 검사와 QC 결정은 별도 사실이며 ERP 상태 변경은 인간 승인 뒤 적용한다.
INSERT INTO allergens (name, code, legal_category, standard)
SELECT v.* FROM (VALUES
-- 1. 곡류 및 두류
('밀', 'ALLERG-01', '밀', 'KR_MFDS'),
('대두', 'ALLERG-02', '대두', 'KR_MFDS'),
('메밀', 'ALLERG-03', '메밀', 'KR_MFDS'),

-- 2. 견과류 및 종실류
('땅콩', 'ALLERG-04', '땅콩', 'KR_MFDS'),
('호두', 'ALLERG-05', '호두', 'KR_MFDS'),
('잣', 'ALLERG-06', '잣', 'KR_MFDS'),

-- 3. 축산물 및 유제품
('난류', 'ALLERG-07', '난류', 'KR_MFDS'),
('우유', 'ALLERG-08', '우유', 'KR_MFDS'),
('쇠고기', 'ALLERG-09', '쇠고기', 'KR_MFDS'),
('돼지고기', 'ALLERG-10', '돼지고기', 'KR_MFDS'),
('닭고기', 'ALLERG-11', '닭고기', 'KR_MFDS'),

-- 4. 수산물 (갑각류, 어류, 연체류)
('고등어', 'ALLERG-12', '고등어', 'KR_MFDS'),
('게', 'ALLERG-13', '게', 'KR_MFDS'),
('새우', 'ALLERG-14', '새우', 'KR_MFDS'),
('오징어', 'ALLERG-15', '오징어', 'KR_MFDS'),

-- 5. 조개류 (법정 1개 군 - 실무 3종 세분화)
('굴', 'ALLERG-16-1', '조개류', 'KR_MFDS'),
('전복', 'ALLERG-16-2', '조개류', 'KR_MFDS'),
('홍합', 'ALLERG-16-3', '조개류', 'KR_MFDS'),

-- 6. 과채류 및 첨가물
('복숭아', 'ALLERG-17', '복숭아', 'KR_MFDS'),
('토마토', 'ALLERG-18', '토마토', 'KR_MFDS'),
('아황산류', 'ALLERG-19', '아황산류', 'KR_MFDS'),

-- 7. 포괄 조개류 (기타 패류 관리용)
('기타조개류', 'ALLERG-16-4', '조개류', 'KR_MFDS')) AS v(name,code,legal_category,standard)
WHERE NOT EXISTS(SELECT 1 FROM allergens a WHERE a.code=v.code AND a.standard=v.standard);

CREATE TABLE material_quality_declarations (
    raw_material_id BIGINT PRIMARY KEY REFERENCES raw_materials(raw_material_id),
    allergen_classification VARCHAR(20) NOT NULL CHECK (allergen_classification IN ('DECLARED','ALLERGEN_FREE')),
    min_temperature NUMERIC(5,2), max_temperature NUMERIC(5,2),
    declared_by BIGINT NOT NULL REFERENCES users(user_id),
    declared_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((min_temperature IS NULL) = (max_temperature IS NULL)),
    CHECK (min_temperature IS NULL OR (min_temperature <> 'NaN'::NUMERIC AND max_temperature <> 'NaN'::NUMERIC AND min_temperature <= max_temperature))
);
CREATE TABLE inbound_inspections (
    inspection_id BIGSERIAL PRIMARY KEY,
    inbound_id BIGINT NOT NULL REFERENCES inbound(inbound_id),
    governance_action_id BIGINT NOT NULL UNIQUE REFERENCES governance_actions(governance_action_id),
    case_id BIGINT NOT NULL REFERENCES cases(case_id),
    work_item_id BIGINT NOT NULL,
    proposed_by_agent_id BIGINT REFERENCES agents(agent_id),
    proposed_by_user_id BIGINT REFERENCES users(user_id),
    version INT NOT NULL CHECK (version > 0),
    proposal_hash CHAR(64) NOT NULL CHECK (proposal_hash ~ '^[0-9a-f]{64}$'),
    snapshot JSONB NOT NULL,
    proposed_status inbound_status NOT NULL CHECK (proposed_status IN ('BLOCKED','RELEASED')),
    FOREIGN KEY (work_item_id,case_id) REFERENCES work_items(work_item_id,case_id),
    CHECK (num_nonnulls(proposed_by_agent_id,proposed_by_user_id)=1),
    UNIQUE(inbound_id,version)
);
CREATE TRIGGER trg_inspection_immutable BEFORE UPDATE OR DELETE ON inbound_inspections
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_modification();
CREATE TRIGGER trg_inspection_no_truncate BEFORE TRUNCATE ON inbound_inspections
    FOR EACH STATEMENT EXECUTE FUNCTION prevent_audit_log_modification();
-- A dedicated inactive service user satisfies the legacy requested_by FK without impersonating
-- the proposing agent. inbound_inspections and audit snapshots retain the actual proposer.
INSERT INTO users(name,email,password,role,is_active)
VALUES ('Quality governance service','quality-service@mulino.internal','disabled-service-identity','VIEWER',false);

CREATE FUNCTION inbound_quality_reasons(receipt BIGINT, as_of DATE) RETURNS TEXT[] AS $$
DECLARE i inbound; d material_quality_declarations; failures TEXT[] := ARRAY[]::TEXT[];
BEGIN
    SELECT * INTO i FROM inbound WHERE inbound_id=receipt;
    IF NOT FOUND THEN RETURN ARRAY['UNKNOWN_INBOUND']; END IF;
    SELECT * INTO d FROM material_quality_declarations WHERE raw_material_id=i.raw_material_id;
    IF NOT FOUND THEN failures:=array_append(failures,'MISSING_ALLERGEN_DECLARATION');
    ELSIF d.allergen_classification='DECLARED' AND (NOT EXISTS(SELECT 1 FROM raw_material_allergens WHERE raw_material_id=i.raw_material_id) OR EXISTS(SELECT 1 FROM raw_material_allergens m JOIN allergens a USING(allergen_id) WHERE m.raw_material_id=i.raw_material_id AND (a.standard<>'KR_MFDS' OR a.code IS NULL OR a.code NOT IN ('ALLERG-01','ALLERG-02','ALLERG-03','ALLERG-04','ALLERG-05','ALLERG-06','ALLERG-07','ALLERG-08','ALLERG-09','ALLERG-10','ALLERG-11','ALLERG-12','ALLERG-13','ALLERG-14','ALLERG-15','ALLERG-16-1','ALLERG-16-2','ALLERG-16-3','ALLERG-16-4','ALLERG-17','ALLERG-18','ALLERG-19'))))
      OR d.allergen_classification='ALLERGEN_FREE' AND EXISTS(SELECT 1 FROM raw_material_allergens WHERE raw_material_id=i.raw_material_id) THEN
      failures:=array_append(failures,'ALLERGEN_CLASSIFICATION_MISMATCH'); END IF;
    IF EXISTS(SELECT 1 FROM unnest(ARRAY['ALLERG-01','ALLERG-02','ALLERG-03','ALLERG-04','ALLERG-05','ALLERG-06','ALLERG-07','ALLERG-08','ALLERG-09','ALLERG-10','ALLERG-11','ALLERG-12','ALLERG-13','ALLERG-14','ALLERG-15','ALLERG-16-1','ALLERG-16-2','ALLERG-16-3','ALLERG-17','ALLERG-18','ALLERG-19','ALLERG-16-4']) expected(code) WHERE NOT EXISTS(SELECT 1 FROM allergens a WHERE a.code=expected.code AND standard='KR_MFDS')) THEN
      failures:=array_append(failures,'INCOMPLETE_KOREAN_ALLERGEN_MASTER'); END IF;
    IF NOT EXISTS(SELECT 1 FROM supplier_certifications WHERE supplier_id=i.supplier_id AND cert_type='HACCP'
      AND issue_date<=i.inbound_date AND expiry_date>=greatest(i.inbound_date,as_of)) THEN
      failures:=array_append(failures,'INVALID_HACCP'); END IF;
    IF NOT EXISTS(SELECT 1 FROM suppliers WHERE supplier_id=i.supplier_id AND is_active) THEN
      failures:=array_append(failures,'INACTIVE_SUPPLIER'); END IF;
    IF NOT EXISTS(SELECT 1 FROM purchase_order_items p JOIN purchase_orders o USING(purchase_order_id)
       WHERE p.purchase_order_item_id=i.purchase_order_item_id AND p.raw_material_id=i.raw_material_id AND o.supplier_id=i.supplier_id) THEN
      failures:=array_append(failures,'RECEIPT_SOURCE_MISMATCH'); END IF;
    IF i.expiry_date IS NULL OR i.expiry_date<greatest(as_of,i.inbound_date) THEN
      failures:=array_append(failures,'EXPIRED_OR_UNKNOWN_EXPIRY'); END IF;
    IF EXISTS(SELECT 1 FROM warehouses WHERE warehouse_id=i.warehouse_id AND type IN ('CHILLED','FROZEN')) AND d.min_temperature IS NULL THEN
      failures:=array_append(failures,'MISSING_TEMPERATURE_POLICY'); END IF;
    IF d.min_temperature IS NOT NULL AND (NOT EXISTS(SELECT 1 FROM inbound_temperature_logs WHERE inbound_id=receipt)
       OR EXISTS(SELECT 1 FROM inbound_temperature_logs WHERE inbound_id=receipt AND (temperature='NaN'::NUMERIC OR temperature<d.min_temperature OR temperature>d.max_temperature))) THEN
      failures:=array_append(failures,'TEMPERATURE_DEVIATION_OR_MISSING'); END IF;
    RETURN failures;
END;
$$ LANGUAGE plpgsql STABLE;

CREATE FUNCTION inbound_quality_eligible(receipt BIGINT, as_of DATE) RETURNS BOOLEAN AS $$
 SELECT EXISTS(SELECT 1 FROM inbound i WHERE i.inbound_id=receipt AND i.status='RELEASED'
    AND cardinality(inbound_quality_reasons(receipt,as_of))=0
    AND NOT EXISTS(SELECT 1 FROM inbound_inspections q JOIN governance_actions a USING(governance_action_id)
        WHERE q.inbound_id=receipt AND a.status IN ('PENDING','BLOCKED','CANCELLED','EXPIRED')
        AND q.version=(SELECT max(version) FROM inbound_inspections WHERE inbound_id=receipt)));
$$ LANGUAGE sql STABLE;

-- Quality facts use the same revision guard as planning and purchasing snapshots.
DO $$ DECLARE t TEXT; BEGIN
 FOREACH t IN ARRAY ARRAY['material_quality_declarations','inbound_inspections','inbound_temperature_logs','raw_material_allergens','allergens','supplier_certifications','inbound','raw_material_lots','suppliers'] LOOP
   EXECUTE format('CREATE TRIGGER trg_quality_source_lock BEFORE INSERT OR UPDATE OR DELETE ON %I FOR EACH STATEMENT EXECUTE FUNCTION lock_planning_data()',t);
 END LOOP;
END $$;

CREATE FUNCTION guard_production_ingredient_quality() RETURNS TRIGGER AS $$
DECLARE lot raw_material_lots; used_delta NUMERIC; production_day DATE;
BEGIN
    IF TG_OP='DELETE' THEN
      UPDATE raw_material_lots SET remaining_quantity=remaining_quantity+OLD.quantity_used WHERE raw_material_lot_id=OLD.raw_material_lot_id;
      RETURN OLD;
    END IF;
    SELECT * INTO lot FROM raw_material_lots WHERE raw_material_lot_id=NEW.raw_material_lot_id FOR UPDATE;
    SELECT p.production_date INTO production_day FROM production_records r JOIN production_lots p ON p.production_lot_id=r.lot_id WHERE r.production_record_id=NEW.production_record_id;
    IF NOT inbound_quality_eligible(lot.inbound_id,production_day) OR lot.expiry_date IS NULL OR lot.expiry_date<production_day OR (SELECT inbound_date FROM inbound WHERE inbound_id=lot.inbound_id)>production_day OR (SELECT warehouse_id FROM inbound WHERE inbound_id=lot.inbound_id)<>(SELECT warehouse_id FROM production_records WHERE production_record_id=NEW.production_record_id) OR EXISTS(SELECT 1 FROM production_records r JOIN production_lots p ON p.production_lot_id=r.lot_id WHERE r.production_record_id=NEW.production_record_id AND p.warehouse_id IS NOT NULL AND p.warehouse_id<>r.warehouse_id) OR lot.raw_material_id<>(SELECT raw_material_id FROM inbound WHERE inbound_id=lot.inbound_id) THEN
      RAISE EXCEPTION 'raw material LOT is not eligible for production' USING ERRCODE='23514'; END IF;
    -- Historical rows are retained. New mutations adjust their delta exactly once.
    IF TG_OP='UPDATE' AND OLD.raw_material_lot_id<>NEW.raw_material_lot_id THEN
      UPDATE raw_material_lots SET remaining_quantity=remaining_quantity+OLD.quantity_used WHERE raw_material_lot_id=OLD.raw_material_lot_id;
      used_delta:=NEW.quantity_used;
    ELSE used_delta:=NEW.quantity_used-CASE WHEN TG_OP='UPDATE' THEN OLD.quantity_used ELSE 0 END; END IF;
    UPDATE raw_material_lots SET remaining_quantity=remaining_quantity-used_delta WHERE raw_material_lot_id=NEW.raw_material_lot_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_quality_production_lock BEFORE INSERT OR UPDATE OR DELETE ON production_ingredients
 FOR EACH STATEMENT EXECUTE FUNCTION lock_planning_data();
CREATE TRIGGER trg_quality_production_guard BEFORE INSERT OR UPDATE OR DELETE ON production_ingredients
 FOR EACH ROW EXECUTE FUNCTION guard_production_ingredient_quality();

CREATE OR REPLACE FUNCTION validate_purchase_attention_scope() RETURNS TRIGGER AS $$
BEGIN
 IF NEW.governance_action_id IS NOT NULL AND NOT (
    EXISTS(SELECT 1 FROM governance_actions a WHERE a.governance_action_id=NEW.governance_action_id AND a.replenishment_plan_id IS NOT NULL AND a.case_id=NEW.case_id AND a.work_item_id=NEW.work_item_id)
    OR EXISTS(SELECT 1 FROM inbound_inspections q WHERE q.governance_action_id=NEW.governance_action_id AND q.case_id=NEW.case_id AND q.work_item_id=NEW.work_item_id)) THEN
    RAISE EXCEPTION 'approval Attention must match its action Case and Work Item' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE FUNCTION protect_quality_action() RETURNS TRIGGER AS $$
BEGIN
 IF TG_OP='DELETE' AND OLD.resource_type='INBOUND_INSPECTION' THEN RAISE EXCEPTION 'quality action is immutable' USING ERRCODE='23514'; END IF;
 IF TG_OP='UPDATE' AND OLD.resource_type='INBOUND_INSPECTION' THEN
   IF (to_jsonb(OLD)-'status') IS DISTINCT FROM (to_jsonb(NEW)-'status') OR (NEW.status<>OLD.status AND NOT (OLD.status='PENDING' AND NEW.status IN ('APPROVED','BLOCKED','CANCELLED','EXPIRED'))) THEN
     RAISE EXCEPTION 'invalid quality action mutation' USING ERRCODE='23514'; END IF;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF; RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_quality_action_guard BEFORE UPDATE OR DELETE ON governance_actions FOR EACH ROW EXECUTE FUNCTION protect_quality_action();

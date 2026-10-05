-- 구매안 취소는 PENDING에서만 허용한다. 기존 이력과 ERP 적용 계약을 보존한다.
ALTER TYPE governance_action_status ADD VALUE 'CANCELLED';

ALTER TABLE work_items DROP CONSTRAINT ck_work_procurement_outcome;
ALTER TABLE work_items ADD CONSTRAINT ck_work_procurement_outcome CHECK (
    (procurement_plan_id IS NULL AND procurement_outcome IS NULL)
    OR (procurement_plan_id IS NOT NULL AND procurement_outcome IS NOT NULL
        AND procurement_outcome IN ('NO_PURCHASE_REQUIRED','PROPOSED','APPLIED','BLOCKED','EXPIRED','CANCELLED')));

CREATE OR REPLACE FUNCTION protect_purchase_proposal() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        IF OLD.replenishment_plan_id IS NOT NULL THEN
            RAISE EXCEPTION 'purchase proposals cannot be deleted' USING ERRCODE='23514';
        END IF;
        RETURN OLD;
    END IF;
    IF TG_OP='UPDATE' AND (OLD.replenishment_plan_id IS NOT NULL OR NEW.replenishment_plan_id IS NOT NULL) THEN
        IF (to_jsonb(OLD)-'status') IS DISTINCT FROM (to_jsonb(NEW)-'status') THEN
            RAISE EXCEPTION 'purchase proposal facts are immutable' USING ERRCODE='23514';
        END IF;
        IF NEW.status IS DISTINCT FROM OLD.status AND NOT (
            OLD.status='PENDING' AND NEW.status::text IN ('APPROVED','BLOCKED','EXPIRED','CANCELLED')) THEN
            RAISE EXCEPTION 'invalid purchase proposal state transition' USING ERRCODE='23514';
        END IF;
    ELSIF TG_OP='INSERT' AND NEW.replenishment_plan_id IS NOT NULL AND NEW.status<>'PENDING' THEN
        RAISE EXCEPTION 'new purchase proposals must start pending' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

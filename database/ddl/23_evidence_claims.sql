-- #44: 원본은 append-only, 상태는 별도 판단 이력으로 보존한다.
ALTER TABLE evidence ADD COLUMN writer_principal TEXT,
    ADD COLUMN writer_user_id BIGINT REFERENCES users(user_id),
    ADD COLUMN writer_agent_id BIGINT REFERENCES agents(agent_id),
    ADD COLUMN corrects_evidence_id BIGINT UNIQUE REFERENCES evidence(evidence_id),
    ADD COLUMN correction_reason TEXT,
    ADD COLUMN observed_instant TIMESTAMPTZ;
ALTER TABLE claims ADD COLUMN writer_principal TEXT,
    ADD COLUMN revision BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN judgment_fingerprint VARCHAR(64),
    ADD COLUMN judgment_stale BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN supersedes_claim_id BIGINT REFERENCES claims(claim_id),
    ADD COLUMN supersession_reason TEXT;
ALTER TABLE claim_evidence ADD COLUMN writer_principal TEXT,
    ADD COLUMN writer_user_id BIGINT REFERENCES users(user_id),
    ADD COLUMN writer_run_id BIGINT REFERENCES runs(run_id);
CREATE TABLE claim_judgments (
    judgment_id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT NOT NULL REFERENCES claims(claim_id),
    revision BIGINT NOT NULL,
    previous_status claim_status NOT NULL,
    previous_resolved_at TIMESTAMP,
    status claim_status NOT NULL,
    reason TEXT NOT NULL,
    evidence_fingerprint VARCHAR(64),
    writer_principal TEXT,
    writer_user_id BIGINT REFERENCES users(user_id),
    writer_run_id BIGINT REFERENCES runs(run_id),
    stale BOOLEAN NOT NULL,
    judged_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (claim_id,revision)
);
CREATE INDEX ix_claim_judgments_claim ON claim_judgments(claim_id,revision);

CREATE FUNCTION evidence_immutable() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Source and judgment history are append-only' USING ERRCODE='23514';
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_evidence_immutable BEFORE UPDATE OR DELETE ON evidence
FOR EACH ROW EXECUTE FUNCTION evidence_immutable();
CREATE TRIGGER trg_evidence_no_truncate BEFORE TRUNCATE ON evidence
FOR EACH STATEMENT EXECUTE FUNCTION evidence_immutable();
CREATE TRIGGER trg_claim_judgments_immutable BEFORE UPDATE OR DELETE ON claim_judgments
FOR EACH ROW EXECUTE FUNCTION evidence_immutable();
CREATE TRIGGER trg_claim_judgments_no_truncate BEFORE TRUNCATE ON claim_judgments
FOR EACH STATEMENT EXECUTE FUNCTION evidence_immutable();
CREATE TRIGGER trg_claim_evidence_immutable BEFORE UPDATE OR DELETE ON claim_evidence
FOR EACH ROW EXECUTE FUNCTION evidence_immutable();
CREATE TRIGGER trg_claim_evidence_no_truncate BEFORE TRUNCATE ON claim_evidence
FOR EACH STATEMENT EXECUTE FUNCTION evidence_immutable();

-- 기존 nullable Case/작성자 행은 유지하며 새 관계만 검사한다.
CREATE FUNCTION evidence_scope() RETURNS TRIGGER AS $$
DECLARE parent_case BIGINT;
BEGIN
    IF NEW.writer_principal IS NOT NULL AND NEW.case_id IS NULL THEN
        RAISE EXCEPTION 'New source requires Case' USING ERRCODE='23514';
    END IF;
    IF NEW.ingested_by_run_id IS NOT NULL THEN
        SELECT case_id INTO parent_case FROM runs WHERE run_id=NEW.ingested_by_run_id FOR SHARE;
        IF NEW.case_id IS DISTINCT FROM parent_case THEN
            RAISE EXCEPTION 'Source Run Case mismatch' USING ERRCODE='23514';
        END IF;
    END IF;
    IF NEW.corrects_evidence_id IS NOT NULL THEN
        SELECT case_id INTO parent_case FROM evidence WHERE evidence_id=NEW.corrects_evidence_id FOR SHARE;
        IF NEW.case_id IS NULL OR NEW.case_id IS DISTINCT FROM parent_case
           OR NEW.correction_reason IS NULL OR btrim(NEW.correction_reason)='' THEN
            RAISE EXCEPTION 'Correction requires same Case and reason' USING ERRCODE='23514';
        END IF;
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_evidence_scope BEFORE INSERT ON evidence FOR EACH ROW EXECUTE FUNCTION evidence_scope();
CREATE FUNCTION claim_source_immutable() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP='UPDATE' AND (NEW.case_id,NEW.subject_type,NEW.subject_ref,NEW.claim_text,
        NEW.asserted_by_agent_id,NEW.asserted_by_user_id,NEW.asserted_by_run_id,NEW.asserted_at,NEW.writer_principal,NEW.supersedes_claim_id,NEW.supersession_reason)
        IS NOT DISTINCT FROM (OLD.case_id,OLD.subject_type,OLD.subject_ref,OLD.claim_text,
        OLD.asserted_by_agent_id,OLD.asserted_by_user_id,OLD.asserted_by_run_id,OLD.asserted_at,OLD.writer_principal,OLD.supersedes_claim_id,OLD.supersession_reason)
        THEN RETURN NEW;
    END IF;
    RAISE EXCEPTION 'Claim assertion is immutable' USING ERRCODE='23514';
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_claim_source_immutable BEFORE UPDATE OR DELETE ON claims
FOR EACH ROW EXECUTE FUNCTION claim_source_immutable();

CREATE FUNCTION claim_link_scope() RETURNS TRIGGER AS $$
DECLARE claim_case BIGINT; source_case BIGINT;
BEGIN
    SELECT case_id INTO claim_case FROM claims WHERE claim_id=NEW.claim_id FOR UPDATE;
    SELECT case_id INTO source_case FROM evidence WHERE evidence_id=NEW.evidence_id FOR SHARE;
    IF source_case IS NULL OR source_case IS DISTINCT FROM claim_case THEN
        RAISE EXCEPTION 'Claim source Case mismatch' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_claim_link_scope BEFORE INSERT ON claim_evidence
FOR EACH ROW EXECUTE FUNCTION claim_link_scope();

CREATE FUNCTION stale_claim_judgment(target BIGINT, rationale TEXT) RETURNS VOID AS $$
DECLARE old_claim claims%ROWTYPE; next_status claim_status;
BEGIN
    SELECT * INTO old_claim FROM claims WHERE claim_id=target FOR UPDATE;
    next_status := CASE WHEN EXISTS(SELECT 1 FROM claim_evidence WHERE claim_id=target AND relation='REFUTES')
        THEN 'CONFLICTED'::claim_status ELSE 'ASSERTED'::claim_status END;
    UPDATE claims SET status=next_status,revision=revision+1,judgment_stale=TRUE,
        resolved_at=CASE WHEN next_status='ASSERTED' THEN NULL ELSE CURRENT_TIMESTAMP END WHERE claim_id=target;
    INSERT INTO claim_judgments(claim_id,revision,previous_status,previous_resolved_at,status,reason,evidence_fingerprint,writer_principal,
        writer_user_id,writer_run_id,stale)
    VALUES(target,old_claim.revision+1,old_claim.status,old_claim.resolved_at,next_status,rationale,old_claim.judgment_fingerprint,
        nullif(current_setting('mulino.evidence_principal',TRUE),''),
        nullif(current_setting('mulino.evidence_user',TRUE),'')::bigint,
        nullif(current_setting('mulino.evidence_run',TRUE),'')::bigint,TRUE);
END; $$ LANGUAGE plpgsql;
CREATE FUNCTION claim_link_stale() RETURNS TRIGGER AS $$
BEGIN
    PERFORM stale_claim_judgment(NEW.claim_id,'LINK_ADDED:'||NEW.evidence_id||':'||NEW.relation);
    RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_claim_link_stale AFTER INSERT ON claim_evidence FOR EACH ROW EXECUTE FUNCTION claim_link_stale();
CREATE FUNCTION correction_stale() RETURNS TRIGGER AS $$
DECLARE target BIGINT;
BEGIN
    IF NEW.corrects_evidence_id IS NOT NULL THEN
        FOR target IN WITH RECURSIVE ancestors AS (
            SELECT evidence_id,corrects_evidence_id FROM evidence WHERE evidence_id=NEW.corrects_evidence_id
            UNION ALL SELECT e.evidence_id,e.corrects_evidence_id FROM evidence e JOIN ancestors a ON e.evidence_id=a.corrects_evidence_id
        ) SELECT DISTINCT ce.claim_id FROM claim_evidence ce JOIN ancestors a USING(evidence_id) ORDER BY ce.claim_id LOOP
            PERFORM stale_claim_judgment(target,'SOURCE_CORRECTED:'||NEW.evidence_id||':'||NEW.correction_reason);
        END LOOP;
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_correction_stale AFTER INSERT ON evidence FOR EACH ROW EXECUTE FUNCTION correction_stale();

CREATE TRIGGER trg_claims_no_truncate BEFORE TRUNCATE ON claims
FOR EACH STATEMENT EXECUTE FUNCTION evidence_immutable();

CREATE FUNCTION claim_run_scope() RETURNS TRIGGER AS $$
DECLARE run_case BIGINT; run_agent BIGINT; predecessor claims%ROWTYPE;
BEGIN
    IF NEW.supersedes_claim_id IS NOT NULL THEN
        SELECT * INTO predecessor FROM claims WHERE claim_id=NEW.supersedes_claim_id FOR SHARE;
        IF NEW.case_id IS DISTINCT FROM predecessor.case_id
            OR NEW.subject_type IS DISTINCT FROM predecessor.subject_type
            OR NEW.subject_ref IS DISTINCT FROM predecessor.subject_ref
            OR NEW.supersession_reason IS NULL OR btrim(NEW.supersession_reason)='' THEN
            RAISE EXCEPTION 'Supersession requires same Case/subject and rationale' USING ERRCODE='23514';
        END IF;
    ELSIF NEW.supersession_reason IS NOT NULL THEN
        RAISE EXCEPTION 'Supersession rationale requires predecessor' USING ERRCODE='23514';
    END IF;
    IF NEW.asserted_by_run_id IS NOT NULL THEN
        SELECT case_id,agent_id INTO run_case,run_agent FROM runs WHERE run_id=NEW.asserted_by_run_id FOR SHARE;
        IF NEW.case_id IS DISTINCT FROM run_case OR
            (NEW.asserted_by_agent_id IS NOT NULL AND NEW.asserted_by_agent_id IS DISTINCT FROM run_agent) THEN
            RAISE EXCEPTION 'Claim Run Case/agent mismatch' USING ERRCODE='23514';
        END IF;
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_claim_run_scope BEFORE INSERT ON claims FOR EACH ROW EXECUTE FUNCTION claim_run_scope();

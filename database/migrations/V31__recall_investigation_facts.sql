-- 회수 실물 처리는 scope version이 아니라 조사(investigation) root 범위의 사실이다.
-- 재-scope 뒤에도 앞 version의 회수·처분·최종 분류를 이어받고, 같은 조사 안의
-- 최종 분류 범위는 version을 넘어 서로소여야 한다(plan §6 반품·회수, D18).
ALTER TABLE mulino_trade_recall_Actions ADD COLUMN investigationId varchar(36);
ALTER TABLE mulino_trade_recall_Actions DISABLE TRIGGER recall_actions_immutable;
UPDATE mulino_trade_recall_Actions a SET investigationId=s.investigationId
 FROM mulino_trade_recall_Scopes s WHERE s.organizationId=a.organizationId AND s.ID=a.scopeId;
ALTER TABLE mulino_trade_recall_Actions ENABLE TRIGGER recall_actions_immutable;
ALTER TABLE mulino_trade_recall_Actions ALTER investigationId SET NOT NULL,
 ADD FOREIGN KEY(organizationId,investigationId) REFERENCES mulino_trade_recall_Investigations(organizationId,ID);
ALTER TABLE mulino_trade_recall_Actions DROP CONSTRAINT recall_terminal_disjoint;
ALTER TABLE mulino_trade_recall_Actions ADD CONSTRAINT recall_terminal_disjoint EXCLUDE USING gist (organizationId WITH =, investigationId WITH =, numrange(startQuantity,startQuantity+quantity,'[)') WITH &&) WHERE (kind IN ('DISPOSED','SAFE','CONSUMED_LOST','EXCEPTION'));
CREATE OR REPLACE FUNCTION mulino_recall_action_bounds() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE s mulino_trade_recall_Scopes%ROWTYPE; a mulino_trade_recall_Approvals%ROWTYPE;
BEGIN
 SELECT * INTO s FROM mulino_trade_recall_Scopes WHERE organizationId=NEW.organizationId AND ID=NEW.scopeId;
 SELECT * INTO a FROM mulino_trade_recall_Approvals WHERE organizationId=NEW.organizationId AND ID=NEW.approvalId;
 IF NEW.investigationId<>s.investigationId OR NEW.rootSegmentId<>s.rootSegmentId OR NEW.unit<>s.unit OR NEW.startQuantity<s.startQuantity OR NEW.startQuantity+NEW.quantity>s.startQuantity+s.quantity OR a.scopeId<>s.ID OR a.scopeVersion<>s.version OR a.scopeHash<>s.scopeHash OR a.decision<>'APPROVE' THEN RAISE EXCEPTION 'Recall action must match exact approved scope'; END IF;
 IF NEW.kind='EXCEPTION' AND (NEW.reason IS NULL OR NEW.residualDutyId IS NULL) THEN RAISE EXCEPTION 'Recall exception needs reason and retained responsibility'; END IF;
 RETURN NEW;
END $$;
-- 같은 scope의 승인 revision은 하나다. 최신 승인 선택이 행 순서에 의존하지 않는다.
CREATE UNIQUE INDEX recall_approval_revision_once ON mulino_trade_recall_Approvals(organizationId,scopeId,revision);
-- 같은 실제 반품 사건의 임시 관측은 하나다(plan §5.3 같은 사건 재처리로 중복 생성하지 않는다).
CREATE UNIQUE INDEX returns_observation_event_once ON mulino_trade_returns_Observations(organizationId,eventId);

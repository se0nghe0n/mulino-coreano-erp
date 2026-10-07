-- Typed decisions have immutable occurrence identity distinct from physical coordinates.
ALTER TABLE mulino_evidence_CanonicalOccurrences ADD occurrenceIdentity varchar(36), ADD occurrenceSemanticHash varchar(64);
ALTER TABLE mulino_evidence_CanonicalOccurrences ADD CHECK(
 (occurrenceIdentity IS NULL AND occurrenceSemanticHash IS NULL) OR
 (occurrenceIdentity IS NOT NULL AND occurrenceSemanticHash ~ '^[a-f0-9]{64}$'));
DROP INDEX evidence_canonical_original;
CREATE UNIQUE INDEX evidence_canonical_original
 ON mulino_evidence_CanonicalOccurrences(organizationId,physicalScopeId,kind,effectiveFrom)
 WHERE supersedesId IS NULL AND occurrenceIdentity IS NULL;
CREATE UNIQUE INDEX evidence_canonical_typed_original
 ON mulino_evidence_CanonicalOccurrences(organizationId,occurrenceIdentity,kind)
 WHERE supersedesId IS NULL AND occurrenceIdentity IS NOT NULL;

-- Existing physical receipt deduplication is unchanged; only installed typed subjects expand.
DO $$
DECLARE relation_name text; constraint_name text;
BEGIN
 FOR relation_name IN SELECT unnest(ARRAY['mulino_evidence_documentversions','mulino_evidence_events',
   'mulino_evidence_claims','mulino_evidence_inboxrecords','mulino_evidence_canonicaloccurrences']) LOOP
  FOR constraint_name IN SELECT conname FROM pg_constraint
    WHERE conrelid=relation_name::regclass AND contype='c'
      AND pg_get_constraintdef(oid) LIKE '%subjectkind%ANY%' LOOP
    EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I',relation_name,constraint_name);
  END LOOP;
  EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I CHECK(subjectKind IN (''ITEM'',''LOT'',''SEGMENT'',''WORK'',''PLACE'',''PURCHASE_ORDER''))',relation_name,relation_name||'_typed_subject');
 END LOOP;
END $$;
CREATE FUNCTION mulino_validate_purchase_evidence_subject() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.subjectKind='PURCHASE_ORDER' AND NOT EXISTS(
   SELECT 1 FROM mulino_trade_purchase_Orders WHERE organizationId=NEW.organizationId AND ID=NEW.subjectId) THEN
   RAISE EXCEPTION 'Purchase evidence requires organization-scoped immutable order' USING ERRCODE='23503';
 END IF;
 RETURN NEW;
END $$;
DO $$
DECLARE relation_name text;
BEGIN
 FOR relation_name IN SELECT unnest(ARRAY['mulino_evidence_documentversions','mulino_evidence_events',
   'mulino_evidence_claims','mulino_evidence_inboxrecords','mulino_evidence_canonicaloccurrences']) LOOP
  EXECUTE format('CREATE TRIGGER typed_subject BEFORE INSERT ON %I FOR EACH ROW EXECUTE FUNCTION mulino_validate_purchase_evidence_subject()',relation_name);
 END LOOP;
END $$;

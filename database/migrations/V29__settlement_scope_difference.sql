-- D19: an invoice whose cumulative matched quantity overlaps an already matched
-- receipt/delivery contribution is a separate settlement difference, never a new
-- fulfillment. Matches stay immutable (V27 trigger); this only adds the explicit fact.
-- Fresh-system note: rows matched before V29 never evaluated overlap. No such rows
-- exist in any recorded environment; the default only satisfies ADD COLUMN.
ALTER TABLE mulino_trade_settlement_Matches ADD COLUMN scopeDifference BOOLEAN NOT NULL DEFAULT FALSE;

-- Status is derived from the stored differences, and every DIFFERENCE keeps its human duty.
ALTER TABLE mulino_trade_settlement_Matches ADD CONSTRAINT settlement_match_status CHECK(
  status IN ('MATCHED','DIFFERENCE')
  AND (status='DIFFERENCE')=(scopeDifference OR currencyDifference OR quantityDifference<>0 OR priceDifference<>0 OR coalesce(originalDifference,0)<>0)
  AND (status='DIFFERENCE')=(dutyRootId IS NOT NULL));

-- Credit notes and corrections are references to an original invoice. They need human
-- review and must not be matched as fresh receipt/delivery quantity.
CREATE FUNCTION mulino_settlement_reject_correction_match() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF EXISTS (SELECT 1 FROM mulino_trade_settlement_Invoices i WHERE i.organizationId=NEW.organizationId AND i.ID=NEW.invoiceId AND i.invoiceKind IN ('CREDIT_NOTE','CORRECTION')) THEN
    RAISE EXCEPTION 'SETTLEMENT_CORRECTION_REVIEW_REQUIRED' USING ERRCODE='23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER settlement_match_rejects_correction BEFORE INSERT ON mulino_trade_settlement_Matches FOR EACH ROW EXECUTE FUNCTION mulino_settlement_reject_correction_match();

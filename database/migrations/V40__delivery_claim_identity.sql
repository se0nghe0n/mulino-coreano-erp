-- A report may name an unknown or unrelated root. Keep the V28 verified FK intact.
-- Existing rows have no recoverable original request: deliberately no backfill.
ALTER TABLE mulino_trade_sales_Observations
 ADD COLUMN claimedRangeRootId varchar(36),
 ADD COLUMN claimPayloadJson text,
 ALTER COLUMN rangeRootId DROP NOT NULL;
ALTER TABLE mulino_trade_sales_Observations
 ADD CONSTRAINT delivery_claim_capture_pair CHECK
  ((claimedRangeRootId IS NULL) = (claimPayloadJson IS NULL)),
 ADD CONSTRAINT delivery_confirmed_root CHECK
  (state <> 'CONFIRMED' OR rangeRootId IS NOT NULL),
 ADD CONSTRAINT delivery_confirmed_claim_identity CHECK
  (state <> 'CONFIRMED' OR claimedRangeRootId IS NULL OR claimedRangeRootId = rangeRootId);

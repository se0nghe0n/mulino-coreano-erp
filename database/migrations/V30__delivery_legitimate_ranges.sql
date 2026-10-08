-- s4-sales-05 (plan §4.2, §4.3): a delivery correction must intersect the exact legitimate
-- coordinates recorded at confirmation with the corrected interval, not assume the missing
-- units were the ineligible part. Deliveries stay immutable (V23 trigger); DeliveryCommands
-- writes the coordinates once at insert as a JSON array of {startQuantity,endQuantity} in the
-- dispatch range root coordinates.
-- The column is nullable: rows recorded before V30 (and direct fixtures of other modules) have
-- no coordinates. DeliveryCorrection treats missing coordinates, or coordinates whose total
-- differs from legitimateQuantity, as unknown and uses the conservative lower bound
-- max(0, legitimate-(original-corrected)), so such a row never gains recognized quantity.
ALTER TABLE mulino_trade_sales_Deliveries ADD COLUMN legitimateRangesJson text;
ALTER TABLE mulino_trade_sales_Deliveries ADD CONSTRAINT sales_delivery_legitimate_ranges
  CHECK(legitimateRangesJson IS NULL OR jsonb_typeof(legitimateRangesJson::jsonb)='array');

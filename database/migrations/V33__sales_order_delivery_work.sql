-- User decision 2026-10-11 UD-1 (plan §6): a sales order creates its own delivery Work and the unit price is an optional
-- input. A line states price and currency together or neither; settlement treats a line without a price as unpriced.
ALTER TABLE mulino_trade_sales_OrderLines ALTER COLUMN price DROP NOT NULL;
ALTER TABLE mulino_trade_sales_OrderLines ALTER COLUMN currency DROP NOT NULL;
ALTER TABLE mulino_trade_sales_OrderLines ADD CONSTRAINT sales_orderlines_price_currency_together
 CHECK ((price IS NULL) = (currency IS NULL));

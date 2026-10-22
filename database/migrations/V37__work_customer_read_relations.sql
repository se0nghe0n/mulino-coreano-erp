-- D01: typed read relations may cross from physical subjects to their Work and
-- customer. Retain same-organization endpoint, definition and cardinality gates.
CREATE OR REPLACE FUNCTION inventory_endpoint_exists(org VARCHAR, typ VARCHAR, objectId VARCHAR) RETURNS BOOLEAN LANGUAGE plpgsql AS $$
BEGIN
 CASE typ
 WHEN 'Product' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_Products WHERE organizationId=org AND ID=objectId);
 WHEN 'TradeItem' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_TradeItems WHERE organizationId=org AND ID=objectId);
 WHEN 'ManufacturingLot' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_ManufacturingLots WHERE organizationId=org AND ID=objectId);
 WHEN 'Manufacturer' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_Manufacturers WHERE organizationId=org AND ID=objectId);
 WHEN 'QuantitySegment' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_QuantitySegments WHERE organizationId=org AND ID=objectId);
 WHEN 'Place' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_Places WHERE organizationId=org AND ID=objectId);
 WHEN 'LogisticsUnit' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_LogisticsUnits WHERE organizationId=org AND ID=objectId);
 WHEN 'Work' THEN RETURN EXISTS(SELECT 1 FROM mulino_work_read_Works WHERE organizationId=org AND ID=objectId);
 WHEN 'Customer' THEN RETURN EXISTS(SELECT 1 FROM mulino_trade_sales_Customers WHERE organizationId=org AND ID=objectId);
 ELSE RETURN FALSE;
 END CASE;
END $$;

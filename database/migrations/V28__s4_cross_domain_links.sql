-- S4 domain graphs are organization-scoped; all effects join their originating exact objects.
ALTER TABLE mulino_trade_sales_Orders ADD FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_OrderRevisions ADD FOREIGN KEY(organizationId,orderId) REFERENCES mulino_trade_sales_Orders(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_OrderRevisions ADD FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_OrderRevisions ADD FOREIGN KEY(organizationId,customerId) REFERENCES mulino_trade_sales_Customers(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_OrderLines ADD FOREIGN KEY(organizationId,orderId) REFERENCES mulino_trade_sales_Orders(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_OrderLines ADD FOREIGN KEY(organizationId,revisionId) REFERENCES mulino_trade_sales_OrderRevisions(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_OrderLines ADD FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_OrderLines ADD FOREIGN KEY(organizationId,customerId) REFERENCES mulino_trade_sales_Customers(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_OrderLines ADD FOREIGN KEY(organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_OrderLines ADD FOREIGN KEY(organizationId,destinationId) REFERENCES mulino_inventory_Places(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_OrderLines ADD FOREIGN KEY(organizationId,previousLineId) REFERENCES mulino_trade_sales_OrderLines(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Observations ADD FOREIGN KEY(organizationId,eventId) REFERENCES mulino_evidence_Events(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Observations ADD FOREIGN KEY(organizationId,dispatchId) REFERENCES mulino_inventory_Dispatches(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Observations ADD FOREIGN KEY(organizationId,cargoScopeId) REFERENCES mulino_inventory_CargoScopes(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Observations ADD FOREIGN KEY(organizationId,salesLineId) REFERENCES mulino_trade_sales_OrderLines(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Observations ADD FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Observations ADD FOREIGN KEY(organizationId,customerId) REFERENCES mulino_trade_sales_Customers(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Observations ADD FOREIGN KEY(organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Observations ADD FOREIGN KEY(organizationId,lotId) REFERENCES mulino_inventory_ManufacturingLots(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Observations ADD FOREIGN KEY(organizationId,rangeRootId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Observations ADD FOREIGN KEY(organizationId,placeId) REFERENCES mulino_inventory_Places(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,observationId) REFERENCES mulino_trade_sales_Observations(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,canonicalOccurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,dispatchId) REFERENCES mulino_inventory_Dispatches(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,cargoScopeId) REFERENCES mulino_inventory_CargoScopes(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,salesLineId) REFERENCES mulino_trade_sales_OrderLines(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,customerId) REFERENCES mulino_trade_sales_Customers(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,lotId) REFERENCES mulino_inventory_ManufacturingLots(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,rangeRootId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,placeId) REFERENCES mulino_inventory_Places(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_Deliveries ADD FOREIGN KEY(organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_ExecutionEffects ADD FOREIGN KEY(organizationId,lineId) REFERENCES mulino_trade_sales_OrderLines(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_DeliveryCorrections ADD FOREIGN KEY(organizationId,deliveryId) REFERENCES mulino_trade_sales_Deliveries(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_sales_DeliveryCorrections ADD FOREIGN KEY(organizationId,canonicalOccurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_Dispatches ADD FOREIGN KEY(organizationId,cargoScopeId) REFERENCES mulino_inventory_CargoScopes(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_Dispatches ADD FOREIGN KEY(organizationId,salesLineId) REFERENCES mulino_trade_sales_OrderLines(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_Dispatches ADD FOREIGN KEY(organizationId,customerId) REFERENCES mulino_trade_sales_Customers(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_Dispatches ADD FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_Dispatches ADD FOREIGN KEY(organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_Dispatches ADD FOREIGN KEY(organizationId,lotId) REFERENCES mulino_inventory_ManufacturingLots(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_Dispatches ADD FOREIGN KEY(organizationId,rangeRootId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_Dispatches ADD FOREIGN KEY(organizationId,destinationId) REFERENCES mulino_inventory_Places(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_CargoScopes ADD FOREIGN KEY(organizationId,rangeRootId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_CargoScopes ADD FOREIGN KEY(organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_CargoScopes ADD FOREIGN KEY(organizationId,customerId) REFERENCES mulino_trade_sales_Customers(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_CargoScopes ADD FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_DeliveryTransfers ADD FOREIGN KEY(organizationId,canonicalId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_inventory_DeliveryTransfers ADD FOREIGN KEY(organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_returns_Authorizations ADD FOREIGN KEY(organizationId,deliveryId) REFERENCES mulino_trade_sales_Deliveries(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_returns_Authorizations ADD FOREIGN KEY(organizationId,customerId) REFERENCES mulino_trade_sales_Customers(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_returns_Authorizations ADD FOREIGN KEY(organizationId,rangeRootId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_returns_Observations ADD FOREIGN KEY(organizationId,deliveryId) REFERENCES mulino_trade_sales_Deliveries(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_returns_Observations ADD FOREIGN KEY(organizationId,customerId) REFERENCES mulino_trade_sales_Customers(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_returns_Observations ADD FOREIGN KEY(organizationId,rangeRootId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_returns_Receipts ADD FOREIGN KEY(organizationId,deliveryId) REFERENCES mulino_trade_sales_Deliveries(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_returns_Receipts ADD FOREIGN KEY(organizationId,customerId) REFERENCES mulino_trade_sales_Customers(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_returns_Receipts ADD FOREIGN KEY(organizationId,rangeRootId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_recall_Actions ADD FOREIGN KEY(organizationId,returnId) REFERENCES mulino_trade_returns_Receipts(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE mulino_trade_recall_Actions ADD FOREIGN KEY(organizationId,residualDutyId) REFERENCES mulino_work_read_ObligationReferences(organizationId,ID) DEFERRABLE INITIALLY DEFERRED;

-- New subject vocabulary does not permit dynamic entity names or cross-tenant references.
DO $$
DECLARE relation_name text; constraint_name text;
BEGIN
 FOR relation_name IN SELECT unnest(ARRAY['mulino_evidence_documentversions','mulino_evidence_events',
   'mulino_evidence_claims','mulino_evidence_inboxrecords','mulino_evidence_canonicaloccurrences']) LOOP
  FOR constraint_name IN SELECT conname FROM pg_constraint WHERE conrelid=relation_name::regclass
    AND contype='c' AND pg_get_constraintdef(oid) LIKE '%subjectkind%ANY%' LOOP
    EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I',relation_name,constraint_name);
  END LOOP;
  EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I CHECK(subjectKind IN (''ITEM'',''LOT'',''SEGMENT'',''WORK'',''PLACE'',''PURCHASE_ORDER'',''PURCHASE_ORDER_LINE'',''SALES_ORDER'',''SALES_ORDER_LINE'',''DISPATCH'',''CARGO_SCOPE'',''DELIVERY'',''DELIVERY_OBSERVATION'',''RETURN'',''RECALL'',''RECALL_SCOPE'',''INVOICE''))',relation_name,relation_name||'_typed_subject');
 END LOOP;
END $$;
CREATE FUNCTION mulino_validate_s4_evidence_subject() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE entity_name text; present boolean;
BEGIN
 entity_name:=CASE NEW.subjectKind
  WHEN 'PURCHASE_ORDER_LINE' THEN 'mulino_trade_purchase_orderlines'
  WHEN 'SALES_ORDER' THEN 'mulino_trade_sales_orders'
  WHEN 'SALES_ORDER_LINE' THEN 'mulino_trade_sales_orderlines'
  WHEN 'DISPATCH' THEN 'mulino_inventory_dispatches'
  WHEN 'CARGO_SCOPE' THEN 'mulino_inventory_cargoscopes'
  WHEN 'DELIVERY' THEN 'mulino_trade_sales_deliveries'
  WHEN 'DELIVERY_OBSERVATION' THEN 'mulino_trade_sales_observations'
  WHEN 'RETURN' THEN 'mulino_trade_returns_observations'
  WHEN 'RECALL' THEN 'mulino_trade_recall_investigations'
  WHEN 'RECALL_SCOPE' THEN 'mulino_trade_recall_scopes'
  WHEN 'INVOICE' THEN 'mulino_trade_settlement_invoices'
  ELSE NULL END;
 IF entity_name IS NOT NULL THEN
  EXECUTE format('SELECT EXISTS(SELECT 1 FROM %I WHERE organizationId=$1 AND ID=$2)',entity_name)
   INTO present USING NEW.organizationId,NEW.subjectId;
  IF NOT present THEN RAISE EXCEPTION 'S4 evidence requires organization-scoped persisted subject' USING ERRCODE='23503'; END IF;
 END IF;
 RETURN NEW;
END $$;
DO $$
DECLARE relation_name text;
BEGIN
 FOR relation_name IN SELECT unnest(ARRAY['mulino_evidence_documentversions','mulino_evidence_events',
   'mulino_evidence_claims','mulino_evidence_inboxrecords','mulino_evidence_canonicaloccurrences']) LOOP
  EXECUTE format('CREATE TRIGGER s4_typed_subject BEFORE INSERT ON %I FOR EACH ROW EXECUTE FUNCTION mulino_validate_s4_evidence_subject()',relation_name);
 END LOOP;
END $$;

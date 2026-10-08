-- §6 정산·D13/D19: a match compares the invoice with the current recognized line contribution,
-- not the actual receipt/delivery. A fully excess receipt or a delivery corrected to zero has a
-- recognized contribution of 0; billing it is a settlement difference with a human duty, so the
-- matched and recognized quantities may be 0. Every other V27 match invariant is unchanged.
DO $$
DECLARE n text;
BEGIN
  SELECT conname INTO STRICT n FROM pg_constraint
   WHERE conrelid='mulino_trade_settlement_matches'::regclass AND contype='c'
     AND pg_get_constraintdef(oid) LIKE '%quantity <= receivedquantity%';
  EXECUTE format('ALTER TABLE mulino_trade_settlement_Matches DROP CONSTRAINT %I', n);
END $$;
ALTER TABLE mulino_trade_settlement_Matches ADD CONSTRAINT settlement_match_quantities CHECK(
  quantity>=0 AND receivedQuantity>=0 AND quantity<=receivedQuantity AND quantity<=orderedQuantity AND quantity<=invoiceQuantity
  AND quantityDifference=invoiceQuantity-receivedQuantity AND currencyDifference=(currency<>invoiceCurrency)
  AND ((currencyDifference AND originalDifference IS NULL) OR (NOT currencyDifference AND originalDifference=invoiceAmount-orderedAmount)));

-- One external remittance may settle several invoices; it is recorded once per invoice.
DO $$
DECLARE n text;
BEGIN
  SELECT conname INTO STRICT n FROM pg_constraint
   WHERE conrelid='mulino_trade_settlement_paymentreferences'::regclass AND contype='u'
     AND pg_get_constraintdef(oid)='UNIQUE (organizationid, externalpaymentreference)';
  EXECUTE format('ALTER TABLE mulino_trade_settlement_PaymentReferences DROP CONSTRAINT %I', n);
END $$;
ALTER TABLE mulino_trade_settlement_PaymentReferences ADD CONSTRAINT settlement_payment_reference_per_invoice UNIQUE(organizationId,invoiceId,externalPaymentReference);

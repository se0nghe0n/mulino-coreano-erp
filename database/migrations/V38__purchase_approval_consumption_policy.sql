-- Preserve historical NULL: no original consumption policy was recorded.
-- A new approval must explicitly persist the supported server-validated policy.
ALTER TABLE mulino_trade_purchase_Approvals
  ADD consumptionPolicy varchar(40)
  CHECK (consumptionPolicy IS NULL OR consumptionPolicy = 'SINGLE_ORDER_REVISION');

CREATE FUNCTION mulino_purchase_require_consumption_policy() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.consumptionPolicy IS DISTINCT FROM 'SINGLE_ORDER_REVISION' THEN
    RAISE EXCEPTION 'New purchase approvals require SINGLE_ORDER_REVISION'
      USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER purchase_approvals_require_consumption_policy
  BEFORE INSERT ON mulino_trade_purchase_Approvals
  FOR EACH ROW EXECUTE FUNCTION mulino_purchase_require_consumption_policy();

-- Only this typed root pins its first assessment proof permanently.
-- Assignment transfer/splitting remains in the existing assignment/scope tables.
CREATE FUNCTION mulino_goal_shortfall_root_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.kind = 'GOAL_QUANTITY_SHORTFALL' OR NEW.kind = 'GOAL_QUANTITY_SHORTFALL' THEN
  RAISE EXCEPTION 'Goal quantity shortfall root proof is immutable' USING ERRCODE = '23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER goal_shortfall_root_immutable BEFORE UPDATE ON mulino_responsibility_Roots
 FOR EACH ROW EXECUTE FUNCTION mulino_goal_shortfall_root_immutable();

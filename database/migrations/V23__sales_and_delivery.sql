-- Immutable commercial revisions, observed reports and actual delivery facts are distinct.
CREATE TABLE mulino_trade_sales_Customers (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 effectiveAt timestamptz NOT NULL,
 name varchar(240) NOT NULL,
 PRIMARY KEY(organizationId,ID)
);
CREATE TABLE mulino_trade_sales_Orders (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 effectiveAt timestamptz NOT NULL,
 workId varchar(36) NOT NULL,
 currentRevision integer NOT NULL,
 PRIMARY KEY(organizationId,ID)
);
CREATE TABLE mulino_trade_sales_OrderRevisions (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 effectiveAt timestamptz NOT NULL,
 orderId varchar(36) NOT NULL,
 workId varchar(36) NOT NULL,
 customerId varchar(36) NOT NULL,
 revisionNumber integer NOT NULL,
 reason varchar(500) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 UNIQUE(organizationId,orderId,revisionNumber)
);
CREATE TRIGGER sales_orderrevisions_immutable BEFORE UPDATE OR DELETE ON mulino_trade_sales_OrderRevisions
 FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_trade_sales_OrderLines (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 effectiveAt timestamptz NOT NULL,
 orderId varchar(36) NOT NULL,
 revisionId varchar(36) NOT NULL,
 workId varchar(36) NOT NULL,
 customerId varchar(36) NOT NULL,
 itemId varchar(36) NOT NULL,
 quantity numeric(38,12) NOT NULL,
 unit varchar(40) NOT NULL,
 price numeric(38,12) NOT NULL,
 currency varchar(3) NOT NULL,
 dueAt timestamptz NOT NULL,
 destinationId varchar(36) NOT NULL,
 deliveryEndpoint varchar(40) NOT NULL,
 qualityTerms varchar(500) NOT NULL,
 packageTerms varchar(500) NOT NULL,
 previousLineId varchar(36),
 PRIMARY KEY(organizationId,ID),
 CHECK(quantity>0 AND price>=0)
);
CREATE TRIGGER sales_orderlines_immutable BEFORE UPDATE OR DELETE ON mulino_trade_sales_OrderLines
 FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_trade_sales_Observations (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 effectiveAt timestamptz NOT NULL,
 eventId varchar(36) NOT NULL,
 dispatchId varchar(36),
 cargoScopeId varchar(36),
 salesLineId varchar(36),
 workId varchar(36) NOT NULL,
 customerId varchar(36) NOT NULL,
 itemId varchar(36) NOT NULL,
 lotId varchar(36),
 rangeRootId varchar(36) NOT NULL,
 startQuantity numeric(38,12) NOT NULL,
 quantity numeric(38,12) NOT NULL,
 unit varchar(40) NOT NULL,
 placeId varchar(36) NOT NULL,
 occurredAt timestamptz NOT NULL,
 state varchar(40) NOT NULL,
 nextCheckAt timestamptz NOT NULL,
 nextAction varchar(320) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 CHECK(quantity>0 AND startQuantity>=0)
);
CREATE TABLE mulino_trade_sales_Deliveries (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 effectiveAt timestamptz NOT NULL,
 observationId varchar(36) NOT NULL,
 canonicalOccurrenceId varchar(36) NOT NULL,
 dispatchId varchar(36) NOT NULL,
 cargoScopeId varchar(36) NOT NULL,
 salesLineId varchar(36) NOT NULL,
 workId varchar(36) NOT NULL,
 customerId varchar(36) NOT NULL,
 itemId varchar(36) NOT NULL,
 lotId varchar(36) NOT NULL,
 rangeRootId varchar(36) NOT NULL,
 startQuantity numeric(38,12) NOT NULL,
 quantity numeric(38,12) NOT NULL,
 unit varchar(40) NOT NULL,
 placeId varchar(36) NOT NULL,
 segmentId varchar(36) NOT NULL,
 occurredAt timestamptz NOT NULL,
 legitimateQuantity numeric(38,12) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 CHECK(quantity>0 AND startQuantity>=0),
 UNIQUE(organizationId,canonicalOccurrenceId),
 CHECK(legitimateQuantity>=0 AND legitimateQuantity<=quantity)
);
CREATE TRIGGER sales_deliveries_immutable BEFORE UPDATE OR DELETE ON mulino_trade_sales_Deliveries
 FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_trade_sales_ExecutionEffects (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 effectiveAt timestamptz NOT NULL,
 lineId varchar(36) NOT NULL,
 kind varchar(40) NOT NULL,
 referenceId varchar(36) NOT NULL,
 quantity numeric(38,12) NOT NULL,
 unit varchar(40) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 UNIQUE(organizationId,lineId,kind,referenceId),
 CHECK(quantity>0)
);
CREATE TRIGGER sales_executioneffects_immutable BEFORE UPDATE OR DELETE ON mulino_trade_sales_ExecutionEffects
 FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_trade_sales_DeliveryCorrections (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 effectiveAt timestamptz NOT NULL,
 deliveryId varchar(36) NOT NULL,
 canonicalOccurrenceId varchar(36) NOT NULL,
 quantity numeric(38,12) NOT NULL,
 legitimateQuantity numeric(38,12) NOT NULL,
 unit varchar(40) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 UNIQUE(organizationId,deliveryId,canonicalOccurrenceId),
 CHECK(quantity>=0 AND legitimateQuantity>=0 AND legitimateQuantity<=quantity)
);
CREATE TRIGGER sales_deliverycorrections_immutable BEFORE UPDATE OR DELETE ON mulino_trade_sales_DeliveryCorrections
 FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();

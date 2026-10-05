package com.mulinocoreano.backend.recall;

import com.mulinocoreano.backend.planning.CanonicalJson;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/** Exact source facts; derived recall eligibility deliberately stays outside the source hash. */
@Service
public class LotTraceService {
  private final JdbcClient jdbc;
  private final CanonicalJson json;

  public LotTraceService(JdbcClient jdbc, CanonicalJson json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  private static final String GRAPH =
      """
      WITH RECURSIVE edges AS (
        SELECT i.source_production_lot_id source,r.lot_id target FROM production_product_inputs i JOIN production_records r USING(production_record_id)
      ), ancestors(id) AS (
        SELECT :lot::bigint UNION SELECT e.source FROM ancestors a JOIN edges e ON e.target=a.id
      ), roots(id) AS (
        SELECT DISTINCT i.raw_material_lot_id FROM production_ingredients i JOIN production_records r USING(production_record_id) JOIN ancestors a ON a.id=r.lot_id
      ), affected(id) AS (
        SELECT :lot::bigint UNION SELECT r.lot_id FROM production_records r JOIN production_ingredients i USING(production_record_id) JOIN roots ON roots.id=i.raw_material_lot_id
        UNION SELECT e.target FROM affected a JOIN edges e ON e.source=a.id
      ), relevant(id) AS (
        SELECT id FROM ancestors UNION SELECT id FROM affected UNION SELECT e.source FROM relevant a JOIN edges e ON e.target=a.id
      ), evidence_raw(id) AS (
        SELECT DISTINCT i.raw_material_lot_id FROM production_ingredients i JOIN production_records r USING(production_record_id) WHERE r.lot_id IN(SELECT id FROM relevant)
      )
      """;

  private JsonNode rows(String sql, long lot) {
    return json.readTree(
        jdbc.sql(
                GRAPH
                    + "SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY t.id),'[]')::text FROM ("
                    + sql
                    + ") t")
            .param("lot", lot)
            .query(String.class)
            .single());
  }

  @Transactional(
      readOnly = true,
      isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
  public JsonNode trace(long lot) {
    if (!jdbc.sql("SELECT EXISTS(SELECT 1 FROM production_lots WHERE production_lot_id=:id)")
        .param("id", lot)
        .query(Boolean.class)
        .single()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    var data = new LinkedHashMap<String, Object>();
    var errors = new TreeSet<String>();
    data.put("incidentLotId", lot);
    var raw =
        rows(
            "SELECT m.*,m.raw_material_lot_id id,(m.raw_material_lot_id IN(SELECT id FROM roots))"
                + " AS \"incidentRoot\",u.unit,i.inbound_id source_inbound_id,i.raw_material_id"
                + " inbound_material_id,i.supplier_id,i.warehouse_id,i.purchase_order_item_id,i.quantity"
                + " inbound_quantity,i.status inbound_status,p.purchase_order_id,p.raw_material_id"
                + " po_material_id,o.supplier_id po_supplier_id FROM raw_material_lots m LEFT JOIN"
                + " raw_materials u USING(raw_material_id) LEFT JOIN inbound i USING(inbound_id)"
                + " LEFT JOIN purchase_order_items p USING(purchase_order_item_id) LEFT JOIN"
                + " purchase_orders o USING(purchase_order_id) WHERE m.raw_material_lot_id"
                + " IN(SELECT id FROM evidence_raw)",
            lot);
    data.put("rawLots", raw);
    data.put(
        "temperatureLogs",
        rows(
            "SELECT t.*,t.inbound_temperature_id id FROM inbound_temperature_logs t WHERE"
                + " inbound_id IN(SELECT inbound_id FROM raw_material_lots WHERE"
                + " raw_material_lot_id IN(SELECT id FROM evidence_raw))",
            lot));
    data.put(
        "inbound",
        rows(
            "SELECT i.*,i.inbound_id id FROM inbound i WHERE i.inbound_id IN(SELECT inbound_id FROM"
                + " raw_material_lots WHERE raw_material_lot_id IN(SELECT id FROM evidence_raw))",
            lot));
    data.put(
        "purchaseOrders",
        rows(
            "SELECT o.*,o.purchase_order_id id FROM purchase_orders o WHERE purchase_order_id"
                + " IN(SELECT purchase_order_id FROM purchase_order_items WHERE"
                + " purchase_order_item_id IN(SELECT purchase_order_item_id FROM inbound WHERE"
                + " inbound_id IN(SELECT inbound_id FROM raw_material_lots WHERE"
                + " raw_material_lot_id IN(SELECT id FROM evidence_raw))))",
            lot));
    data.put(
        "suppliers",
        rows(
            "SELECT s.*,s.supplier_id id FROM suppliers s WHERE supplier_id IN(SELECT supplier_id"
                + " FROM inbound WHERE inbound_id IN(SELECT inbound_id FROM raw_material_lots WHERE"
                + " raw_material_lot_id IN(SELECT id FROM evidence_raw)))",
            lot));
    data.put(
        "purchaseOrderItems",
        rows(
            "SELECT p.*,p.purchase_order_item_id id FROM purchase_order_items p WHERE"
                + " purchase_order_item_id IN(SELECT purchase_order_item_id FROM inbound WHERE"
                + " inbound_id IN(SELECT inbound_id FROM raw_material_lots WHERE"
                + " raw_material_lot_id IN(SELECT id FROM evidence_raw)))",
            lot));
    data.put(
        "products",
        rows(
            "SELECT p.*,p.product_id id FROM products p WHERE product_id IN(SELECT product_id FROM"
                + " production_lots WHERE production_lot_id IN(SELECT id FROM relevant))",
            lot));
    data.put(
        "materials",
        rows(
            "SELECT m.*,m.raw_material_id id FROM raw_materials m WHERE raw_material_id IN(SELECT"
                + " raw_material_id FROM raw_material_lots WHERE raw_material_lot_id IN(SELECT id"
                + " FROM evidence_raw))",
            lot));
    data.put(
        "declarations",
        rows(
            "SELECT d.*,d.raw_material_id id FROM material_quality_declarations d WHERE"
                + " raw_material_id IN(SELECT raw_material_id FROM raw_material_lots WHERE"
                + " raw_material_lot_id IN(SELECT id FROM evidence_raw))",
            lot));
    data.put(
        "certificates",
        rows(
            "SELECT c.*,c.supplier_certification_id id FROM supplier_certifications c WHERE"
                + " supplier_id IN(SELECT supplier_id FROM inbound WHERE inbound_id IN(SELECT"
                + " inbound_id FROM raw_material_lots WHERE raw_material_lot_id IN(SELECT id FROM"
                + " evidence_raw)))",
            lot));
    var lots =
        rows(
            "SELECT p.*,p.production_lot_id id,u.unit,(p.production_lot_id IN(SELECT id FROM"
                + " affected)) affected FROM production_lots p LEFT JOIN products u"
                + " USING(product_id) WHERE production_lot_id IN(SELECT id FROM relevant)",
            lot);
    data.put("productionLots", lots);
    var records =
        rows(
            "SELECT r.*,r.production_record_id id FROM production_records r WHERE lot_id IN(SELECT"
                + " id FROM relevant)",
            lot);
    data.put("productionRecords", records);
    var ingredients =
        rows(
            "SELECT i.*,i.production_ingredient_id id,r.lot_id FROM production_ingredients i JOIN"
                + " production_records r USING(production_record_id) WHERE r.lot_id IN(SELECT id"
                + " FROM relevant)",
            lot);
    data.put("ingredients", ingredients);
    var inputs =
        rows(
            "SELECT i.*,i.production_product_input_id id,r.lot_id target_lot_id,r.warehouse_id"
                + " target_warehouse_id,s.warehouse_id source_warehouse_id,t.warehouse_id"
                + " lot_target_warehouse_id FROM production_product_inputs i JOIN"
                + " production_records r USING(production_record_id) JOIN production_lots s ON"
                + " s.production_lot_id=i.source_production_lot_id JOIN production_lots t ON"
                + " t.production_lot_id=r.lot_id WHERE r.lot_id IN(SELECT id FROM relevant) OR"
                + " i.source_production_lot_id IN(SELECT id FROM relevant)",
            lot);
    data.put("productInputs", inputs);
    var shipments =
        rows(
            "SELECT o.*,o.outbound_id id,c.customer_id,(SELECT coalesce(sum(l.lot_quantity),0) FROM"
                + " outbound_lots l WHERE l.outbound_id=o.outbound_id) allocated_quantity FROM"
                + " outbound o JOIN orders c USING(order_id) WHERE o.outbound_id IN(SELECT"
                + " outbound_id FROM outbound_lots WHERE lot_id IN(SELECT id FROM affected))",
            lot);
    data.put("shipments", shipments);
    data.put(
        "orderItems",
        rows(
            "SELECT i.*,i.orders_item_id id FROM order_items i WHERE order_id IN(SELECT order_id"
                + " FROM outbound WHERE outbound_id IN(SELECT outbound_id FROM outbound_lots WHERE"
                + " lot_id IN(SELECT id FROM affected)))",
            lot));
    var allocations =
        rows(
            "SELECT l.*,l.outbound_lot_id id,p.product_id lot_product_id,p.warehouse_id"
                + " lot_warehouse_id,o.product_id outbound_product_id,o.warehouse_id"
                + " outbound_warehouse_id FROM outbound_lots l LEFT JOIN production_lots p ON"
                + " p.production_lot_id=l.lot_id LEFT JOIN outbound o USING(outbound_id) WHERE"
                + " l.outbound_id IN(SELECT outbound_id FROM outbound_lots WHERE lot_id IN(SELECT"
                + " id FROM affected)) OR l.lot_id IN(SELECT id FROM relevant)",
            lot);
    data.put("allocations", allocations);
    data.put(
        "customers",
        rows(
            "SELECT c.*,c.customer_id id FROM customers c WHERE customer_id IN(SELECT customer_id"
                + " FROM orders WHERE order_id IN(SELECT order_id FROM outbound WHERE outbound_id"
                + " IN(SELECT outbound_id FROM outbound_lots WHERE lot_id IN(SELECT id FROM"
                + " affected))))",
            lot));
    data.put(
        "orders",
        rows(
            "SELECT o.*,o.order_id id FROM orders o WHERE order_id IN(SELECT order_id FROM outbound"
                + " WHERE outbound_id IN(SELECT outbound_id FROM outbound_lots WHERE lot_id"
                + " IN(SELECT id FROM affected)))",
            lot));
    data.put(
        "stock",
        rows(
            "SELECT s.*,s.stock_id id FROM stock s WHERE (product_id,warehouse_id) IN(SELECT"
                + " product_id,warehouse_id FROM production_lots WHERE production_lot_id IN(SELECT"
                + " id FROM affected))",
            lot));
    data.put(
        "warehouses",
        rows(
            "SELECT w.*,w.warehouse_id id FROM warehouses w WHERE warehouse_id IN(SELECT"
                + " warehouse_id FROM production_lots WHERE production_lot_id IN(SELECT id FROM"
                + " relevant) UNION SELECT warehouse_id FROM inbound WHERE inbound_id IN(SELECT"
                + " inbound_id FROM raw_material_lots WHERE raw_material_lot_id IN(SELECT id FROM"
                + " evidence_raw)))",
            lot));
    if (raw.isEmpty()) errors.add("NO_RAW_SOURCE");
    for (var shipment : shipments)
      if (!jdbc.sql(
              "SELECT EXISTS(SELECT 1 FROM order_items WHERE order_id=:order AND"
                  + " product_id=:product)")
          .param("order", shipment.path("order_id").asLong())
          .param("product", shipment.path("product_id").asLong())
          .query(Boolean.class)
          .single()) errors.add("MISSING_ORDER_PRODUCT:" + shipment.path("id").asLong());
    for (var r : raw) {
      if (r.path("unit").isNull()
          || r.path("source_inbound_id").isNull()
          || r.path("purchase_order_id").isNull()
          || r.path("warehouse_id").isNull()
          || r.path("raw_material_id").asLong() != r.path("inbound_material_id").asLong()
          || r.path("raw_material_id").asLong() != r.path("po_material_id").asLong()
          || r.path("supplier_id").asLong() != r.path("po_supplier_id").asLong())
        errors.add("RAW_SOURCE_MISMATCH:" + r.path("id").asLong());
      var used =
          jdbc.sql(
                  "SELECT coalesce(sum(quantity_used),0) FROM production_ingredients WHERE"
                      + " raw_material_lot_id=:id")
              .param("id", r.path("id").asLong())
              .query(BigDecimal.class)
              .single();
      if (decimal(r, "quantity").subtract(used).compareTo(decimal(r, "remaining_quantity")) != 0)
        errors.add("RAW_BALANCE_MISMATCH:" + r.path("id").asLong());
    }
    Map<Long, JsonNode> rawMap = new HashMap<>();
    for (var r : raw) rawMap.put(r.path("id").asLong(), r);
    for (var i : ingredients) {
      var r = rawMap.get(i.path("raw_material_lot_id").asLong());
      JsonNode production = null;
      for (var rec : records)
        if (rec.path("id").asLong() == i.path("production_record_id").asLong()) production = rec;
      if (r == null
          || production == null
          || r.path("warehouse_id").asLong() != production.path("warehouse_id").asLong())
        errors.add("RAW_INPUT_LOCATION_MISMATCH:" + i.path("id").asLong());
    }
    Map<Long, JsonNode> lotMap = new HashMap<>();
    for (var p : lots) lotMap.put(p.path("id").asLong(), p);
    Map<Long, Set<Long>> graph = new HashMap<>();
    for (var i : inputs) {
      long source = i.path("source_production_lot_id").asLong(),
          target = i.path("target_lot_id").asLong();
      graph.computeIfAbsent(source, k -> new HashSet<>()).add(target);
      var s = lotMap.get(source);
      var t = lotMap.get(target);
      if (i.path("source_warehouse_id").isNull()
          || i.path("lot_target_warehouse_id").isNull()
          || i.path("source_warehouse_id").asLong() != i.path("target_warehouse_id").asLong()
          || i.path("lot_target_warehouse_id").asLong() != i.path("target_warehouse_id").asLong())
        errors.add("PRODUCT_INPUT_LOCATION_MISMATCH:" + i.path("id").asLong());
    }
    for (var r : records) {
      var p = lotMap.get(r.path("lot_id").asLong());
      if (p == null
          || p.path("warehouse_id").isNull()
          || p.path("warehouse_id").asLong() != r.path("warehouse_id").asLong())
        errors.add("PRODUCTION_LOCATION_MISMATCH:" + r.path("id").asLong());
    }
    for (long id : lotMap.keySet())
      if (cycle(id, graph, new HashSet<>(), new HashSet<>())) {
        errors.add("PRODUCT_INPUT_CYCLE");
        break;
      }
    var residuals = new ArrayList<Object>();
    for (var p : lots) {
      long id = p.path("id").asLong();
      if (p.path("unit").isNull() || p.path("warehouse_id").isNull())
        errors.add("MISSING_PRODUCT_LOCATION_OR_UNIT:" + id);
      boolean hasInput = false;
      for (var i : ingredients) if (i.path("lot_id").asLong() == id) hasInput = true;
      for (var i : inputs) if (i.path("target_lot_id").asLong() == id) hasInput = true;
      if (!hasInput) errors.add("MISSING_PRODUCTION_SOURCE:" + id);
      BigDecimal shipped = BigDecimal.ZERO, used = BigDecimal.ZERO;
      for (var a : allocations)
        if (a.path("lot_id").asLong() == id) shipped = shipped.add(decimal(a, "lot_quantity"));
      for (var i : inputs)
        if (i.path("source_production_lot_id").asLong() == id)
          used = used.add(decimal(i, "quantity_used"));
      BigDecimal remaining = decimal(p, "quantity").subtract(shipped).subtract(used);
      if (remaining.signum() < 0) errors.add("PRODUCT_OVERALLOCATED:" + id);
      residuals.add(
          Map.of(
              "lotId",
              id,
              "produced",
              decimal(p, "quantity"),
              "shipped",
              shipped,
              "consumed",
              used,
              "remaining",
              remaining,
              "unit",
              p.path("unit").asText(),
              "affected",
              p.path("affected").asBoolean()));
    }
    for (var s : shipments)
      if (decimal(s, "quantity").compareTo(decimal(s, "allocated_quantity")) != 0)
        errors.add("OUTBOUND_ALLOCATION_MISMATCH:" + s.path("id").asLong());
    for (var a : allocations)
      if (a.path("lot_product_id").asLong() != a.path("outbound_product_id").asLong()
          || a.path("lot_warehouse_id").isNull()
          || a.path("lot_warehouse_id").asLong() != a.path("outbound_warehouse_id").asLong())
        errors.add("SHIPMENT_LOT_IDENTITY_MISMATCH:" + a.path("id").asLong());
    Set<String> stockPairs = new HashSet<>();
    for (var p : lots)
      if (p.path("affected").asBoolean() && !p.path("warehouse_id").isNull()) {
        long product = p.path("product_id").asLong(), warehouse = p.path("warehouse_id").asLong();
        if (!stockPairs.add(product + ":" + warehouse)) continue;
        boolean consistent =
            jdbc.sql(
                    """
                        SELECT EXISTS(SELECT 1 FROM stock s WHERE s.product_id=:product AND s.warehouse_id=:warehouse AND s.quantity=(
                        SELECT coalesce(sum(p.quantity-(SELECT coalesce(sum(lot_quantity),0) FROM outbound_lots o WHERE o.lot_id=p.production_lot_id)-(SELECT coalesce(sum(quantity_used),0) FROM production_product_inputs i WHERE i.source_production_lot_id=p.production_lot_id)),0)
                        FROM production_lots p WHERE p.product_id=:product AND p.warehouse_id=:warehouse))
                    """)
                .param("product", product)
                .param("warehouse", warehouse)
                .query(Boolean.class)
                .single();
        if (!consistent) errors.add("STOCK_LOT_MISMATCH:" + product + ":" + warehouse);
      }
    data.put("lotBalances", residuals);
    data.put("problems", errors);
    data.put("complete", errors.isEmpty());
    return json.readTree(json.write(data));
  }

  private static BigDecimal decimal(JsonNode row, String key) {
    return new BigDecimal(row.path(key).asText());
  }

  private static boolean cycle(
      long id, Map<Long, Set<Long>> graph, Set<Long> active, Set<Long> done) {
    if (active.contains(id)) return true;
    if (!done.add(id)) return false;
    active.add(id);
    for (long next : graph.getOrDefault(id, Set.of()))
      if (cycle(next, graph, active, done)) return true;
    active.remove(id);
    return false;
  }
}

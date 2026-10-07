package com.mulino.application.trade.sales;
import com.mulino.application.core.DomainContext;import com.mulino.domain.inventory.StockPrimitives;import java.time.Instant;import java.util.Map;
final class SalesRows {private SalesRows(){}static Map<String,Object> row(DomainContext c,String id,Instant at){var row=StockPrimitives.row(c,id,at);row.put("effectiveAt",at);row.put("revision",1);return row;}}

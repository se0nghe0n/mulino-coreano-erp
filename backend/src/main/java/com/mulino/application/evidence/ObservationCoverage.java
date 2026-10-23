package com.mulino.application.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.domain.definitions.DefinitionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static com.mulino.domain.evidence.EvidenceTypes.*;

/** Source coverage is a closed interval on PostgreSQL's microsecond time grid.
 * Store its exclusive successor, never a nanosecond that PostgreSQL would round
 * away. Equal endpoints are a point: no persisted interval or implied tail.
 * occurredAt is the source report time, independent of its coverage assertion. */
final class ObservationCoverage {
  private ObservationCoverage() {}
  static boolean matches(Map<String,Object> claim,Map<String,Object> event,Map<String,Object> document,
      Map<String,Object> segment,byte[] original,Instant now) {
    try {
      var mapper=new ObjectMapper();var source=mapper.readTree(original);
      if(!source.isObject()||!source.equals(mapper.readTree(event.get("payload").toString())))return false;
      Instant start=Instant.parse(source.path("coverageStart").asText());
      Instant end=Instant.parse(source.path("coverageEnd").asText());
      Instant reported=Instant.parse(source.path("occurredAt").asText());
      if(start.getNano()%1000!=0||end.getNano()%1000!=0||reported.getNano()%1000!=0||end.isBefore(start)
          ||end.isAfter(now)||reported.isAfter(now))return false;
      Instant until=start.equals(end)?null:end.plusNanos(1000);
      return "STATE_OBSERVATION".equals(source.path("activityKind").asText())
        &&"SEGMENT".equals(claim.get("subjectKind"))&&Objects.equals(segment.get("ID"),claim.get("subjectId"))
        &&Objects.equals(segment.get("ID"),source.path("segmentId").asText())
        &&Objects.equals(segment.get("itemId"),claim.get("itemId"))
        &&Objects.equals(segment.get("placeId"),claim.get("placeId"))
        &&Objects.equals(segment.get("placeId"),source.path("placeId").asText())
        &&Objects.equals(segment.get("unit"),source.path("unit").asText())
        &&Objects.equals(claim.get("unit"),source.path("unit").asText())
        &&source.path("quantity").isTextual()
        &&new BigDecimal(source.path("quantity").asText()).compareTo((BigDecimal)claim.get("quantity"))==0
        &&Objects.equals(event.get("sourceNamespace"),source.path("sourceNamespace").asText())
        &&Objects.equals(event.get("sourceVersion"),source.path("sourceVersion").asText())
        &&Objects.equals(document.get("sourceProfileId"),event.get("sourceProfileId"))
        &&Objects.equals(document.get("sourceReference"),event.get("externalEventId"))
        &&start.equals(instant(claim.get("effectiveFrom")))
        &&Objects.equals(until,claim.get("effectiveUntil")==null?null:instant(claim.get("effectiveUntil")))
        &&!start.isBefore(instant(segment.get("validFrom")))
        &&(segment.get("retiredAt")==null||end.isBefore(instant(segment.get("retiredAt"))));
    }catch(Exception invalid){return false;}
  }
  static boolean overlaps(Map<String,Object> left,Map<String,Object> right) {
    Instant a=instant(left.get("effectiveFrom")),b=instant(right.get("effectiveFrom"));
    Instant ae=left.get("effectiveUntil")==null?a.plusNanos(1):instant(left.get("effectiveUntil"));
    Instant be=right.get("effectiveUntil")==null?b.plusNanos(1):instant(right.get("effectiveUntil"));
    return a.isBefore(be)&&b.isBefore(ae);
  }
  static Map<String,Object> canonicalFields(String organization,String physical,Map<String,Object> claim) {
    String coordinate=organization+"|STATE_OBSERVATION|"+physical+"|"+instant(claim.get("effectiveFrom"))
      +"|"+(claim.get("effectiveUntil")==null?"POINT":instant(claim.get("effectiveUntil")))+"|"+claim.get("timePrecision");
    String meaning=coordinate+"|"+claim.get("placeId")+"|"+((BigDecimal)claim.get("quantity")).stripTrailingZeros().toPlainString()+"|"+claim.get("unit");
    return Map.of("occurrenceIdentity",UUID.nameUUIDFromBytes(coordinate.getBytes(StandardCharsets.UTF_8)).toString(),
      "occurrenceSemanticHash",DefinitionRepository.sha256(meaning));
  }
}

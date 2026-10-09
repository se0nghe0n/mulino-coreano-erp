package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.mulino.verification.Json;

/**
 * Default-mode command slot translations the Step 2 contract owners asked the actual adapter to perform
 * (docs/execution/step2r-round9..11 "Step 3 actual adapter" requests, contracts/execution-preconditions commandBasis).
 * Each translation only renames or projects a value the case already states; the adapter never chooses a value.
 * Every applied translation is returned so the receipt records it (adapterTranslations).
 *
 * - dispatchQuantity: corpus slot cargoPlaceId is the product transitPlaceId (round 10).
 * - basis commands (dispatch, split, merge, move, stocktake, adjust, dispose): the request basis
 *   (slots evidenceRef/evidenceId/evidenceIds/evidence, else request evidenceRefs) becomes the product slot evidenceRef
 *   (round 10, commandBasis). Only the first named basis is sent: the product slot holds one reference.
 * - reserveQuantity: projected to the product slot list {segmentId, salesLineId, startQuantity, quantity, unit}; the
 *   segment comes from segmentId/sourceSegmentId or the QuantitySegment subject, the line from orderLineId, salesLineId,
 *   saleLineId, salesOrderLineId, orderId (in that order), startQuantity defaults to "0", a typed {value, unit}
 *   quantity is sent as the decimal string plus the unit slot (round 11).
 * - pickQuantity: projected to {allocationId} (round 9).
 * The provenance map is left as authored: the product validates its values, not its key coverage.
 */
final class ScenarioWireMapping {
    static final Set<String> BASIS=Set.of("dispatchQuantity","splitQuantity","mergeQuantity","moveQuantity","recordStocktake","adjustQuantity","disposeQuantity");
    static final List<String> BASIS_SLOTS=List.of("evidenceRef","evidenceId","evidenceIds","evidence");
    static final List<String> LINE_SLOTS=List.of("orderLineId","salesLineId","saleLineId","salesOrderLineId","orderId");
    private ScenarioWireMapping() {}

    static void command(String capability,ObjectNode request,ArrayNode translated) {
        if(!(request.path("slots") instanceof ObjectNode slots))return;
        if(capability.equals("dispatchQuantity")&&slots.has("cargoPlaceId")&&!slots.has("transitPlaceId")){slots.set("transitPlaceId",id(slots.path("cargoPlaceId")));slots.remove("cargoPlaceId");translated.add("slots.cargoPlaceId->slots.transitPlaceId");}
        if(BASIS.contains(capability)&&!slots.path("evidenceRef").isTextual()) {
            String basis=null;String from=null;
            for(String key:BASIS_SLOTS){JsonNode v=slots.path(key);JsonNode first=v.isArray()?v.path(0):v;String s=text(first);if(s!=null){basis=s;from="slots."+key;break;}}
            if(basis==null){String s=text(request.path("evidenceRefs").path(0));if(s!=null){basis=s;from="evidenceRefs[0]";}}
            if(basis!=null){for(String key:BASIS_SLOTS)slots.remove(key);slots.put("evidenceRef",basis);translated.add(from+"->slots.evidenceRef");}
        }
        if(capability.equals("reserveQuantity")) {
            var out=Json.object();
            String segment=text(slots.path("segmentId"));if(segment==null)segment=text(slots.path("sourceSegmentId"));
            if(segment==null)for(JsonNode ref:request.path("subjectRefs"))if(ref.path("type").asText().equals("QuantitySegment")){segment=ref.path("id").asText(null);break;}
            String line=null;for(String key:LINE_SLOTS){line=text(slots.path(key));if(line!=null)break;}
            if(segment!=null)out.put("segmentId",segment);if(line!=null)out.put("salesLineId",line);
            JsonNode q=slots.path("quantity");String unit=text(slots.path("unit"));
            if(q.isObject()&&q.has("value")){out.put("quantity",q.path("value").asText());if(unit==null)unit=q.path("unit").asText(null);}else if(q.isTextual())out.put("quantity",q.asText());
            if(unit!=null)out.put("unit",unit);
            JsonNode start=slots.path("startQuantity");out.put("startQuantity",start.isObject()?start.path("value").asText():start.isMissingNode()||start.isNull()?"0":start.asText());
            if(!out.equals(slots)){request.set("slots",out);translated.add("reserveQuantity slots projected to segmentId,salesLineId,startQuantity,quantity,unit");}
        }
        if(capability.equals("pickQuantity")&&slots.has("allocationId")&&slots.size()>1){var out=Json.object();out.set("allocationId",id(slots.path("allocationId")));request.set("slots",out);translated.add("pickQuantity slots projected to allocationId");}
    }
    private static JsonNode id(JsonNode v){return v.isObject()&&v.has("id")?v.path("id"):v;}
    private static String text(JsonNode v){if(v==null||v.isMissingNode()||v.isNull())return null;if(v.isObject()&&v.has("id"))return v.path("id").asText(null);return v.isTextual()?v.asText():null;}
}

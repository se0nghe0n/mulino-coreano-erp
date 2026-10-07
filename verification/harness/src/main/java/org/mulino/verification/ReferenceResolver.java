package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/** Strict identity bindings and bounded input mutation. No evaluation or business arithmetic. */
public final class ReferenceResolver {
    private final Map<String,JsonNode> results;
    private final JsonNode aliases;
    public ReferenceResolver(Map<String,JsonNode> results,JsonNode aliases){this.results=results;this.aliases=aliases;}
    public JsonNode resolve(JsonNode node){return resolve(node,false);}
    public JsonNode identity(JsonNode node){return resolve(node,true);}
    private JsonNode resolve(JsonNode node,boolean identity) {
        if(node.isObject() && node.has("$alias")) {
            require(node.size()==1 && node.path("$alias").isTextual(),"Alias reference requires only textual $alias");
            JsonNode value=aliases.get(node.path("$alias").asText());require(value!=null && !value.isNull(),"Unresolved server fixture alias "+node.path("$alias"));
            if(identity) require(nonemptyString(value),"Assertion alias must be actual string ID");return value.deepCopy();
        }
        if(node.isObject() && node.has("$result")) {
            require(node.size()==1,"Result reference allows only $result");JsonNode ref=node.path("$result");
            require(ref.isObject() && ref.size()==2 && ref.has("actionId") && ref.has("pointer"),"Result reference requires only actionId/pointer");
            if(identity) require(identityPointer(ref.path("pointer").asText()),"Assertion references must bind identity, not observed business values");
            JsonNode value=new AssertionEngine().select(ref,results,false);
            if(identity) checkIdentityValue(ref.path("pointer").asText(),value);
            return value.deepCopy();
        }
        if(node.isObject() && node.has("$transform")) {
            require(!identity,"Assertion identity bindings cannot transform expected values");
            require(node.size()==1,"Transform reference allows only $transform");return transform(node.path("$transform"));
        }
        if(node.isObject()) {ObjectNode out=Json.object();node.fields().forEachRemaining(e->out.set(e.getKey(),resolve(e.getValue(),identity)));return out;}
        if(node.isArray()) {var out=Json.array();node.forEach(n->out.add(resolve(n,identity)));return out;}
        return node.deepCopy();
    }
    private static final Set<String> ID_FIELDS=Set.of(
        "id","objectId","itemId","lotId","segmentId","workId","activityId","obligationId",
        "ownerId","actorId","principalId","subjectId","organizationId","tenantId","commandId",
        "requestId","proposalId","approvalId","restrictionId","allocationId","movementId","evidenceId",
        "occurrenceId","definitionId","evaluatorId","policyId","grantId","taskId","runId","workLinkId",
        "parentWorkId","childWorkId","supplierId","customerId","purchaseOrderId","shipmentId","invoiceId","externalId",
        "runtimeTaskId","invocationHandle","transactionId","goalVersionId");
    private static final Set<String> IDS_FIELDS=Set.of("ids","workIds","obligationIds");
    private static final Set<String> HASH_FIELDS=Set.of("hash","proposalHash","evidenceHash","definitionHash","policyHash","artifactHash","requestHash","inputHash","sha256");
    private static final Set<String> REVISION_FIELDS=Set.of("revision","proposalRevision","snapshotRevision","definitionRevision","policyRevision","grantRevision","workRevision","approvalRevision");
    private static String identityField(String pointer) {return pointer.substring(pointer.lastIndexOf('/')+1);}
    public static boolean identityPointer(String pointer) {
        if(!pointer.startsWith("/")) return false;
        String field=identityField(pointer);
        return ID_FIELDS.contains(field) || IDS_FIELDS.contains(field) || HASH_FIELDS.contains(field) || REVISION_FIELDS.contains(field);
    }
    private static void checkIdentityValue(String pointer,JsonNode value) {
        String field=identityField(pointer);
        if(ID_FIELDS.contains(field)) require(nonemptyString(value),"Actual identity ID must be nonempty string, not quantity/Boolean/object");
        else if(IDS_FIELDS.contains(field)) {
            require(value.isArray() && !value.isEmpty(),"Actual identity IDs must be nonempty string array");
            for(JsonNode id:value) require(nonemptyString(id),"Actual identity IDs must contain strings only");
        } else if(HASH_FIELDS.contains(field)) require(value.isTextual() && value.asText().matches("[a-fA-F0-9]{64}"),"Actual identity hash must be SHA-256 hex string");
        else if(REVISION_FIELDS.contains(field)) require(nonemptyString(value) || (value.isIntegralNumber() && value.canConvertToLong() && value.longValue()>=0),"Actual identity revision must be nonempty string or nonnegative integer");
        else throw new IllegalArgumentException("Unsupported assertion identity field "+field);
    }
    private static boolean nonemptyString(JsonNode value) {return value.isTextual() && !value.asText().isBlank() && value.asText().length()<=512;}
    private JsonNode transform(JsonNode t) {
        require(t.isObject() && t.path("source").isObject() && t.path("source").has("$result"),"Transform source must be actual strict $result");
        JsonNode source=resolve(t.path("source"));String op=Json.required(t,"operation");
        switch(op) {
            case "opaqueByteXor": {
                require(t.size()==4 && t.has("index") && t.has("xor"),"opaqueByteXor accepts source/operation/index/xor only");
                require(source.isTextual(),"opaqueByteXor requires opaque string");byte[] bytes=source.asText().getBytes(StandardCharsets.UTF_8);
                require(bytes.length>0 && bytes.length<=65536,"Opaque input length outside bound");
                require(t.path("index").isIntegralNumber() && t.path("index").canConvertToInt() && t.path("xor").isIntegralNumber() && t.path("xor").canConvertToInt(),"Byte position/mask must be integers");
                int index=t.path("index").asInt(-1),mask=t.path("xor").asInt(0);require(index>=0 && index<bytes.length && mask>=1 && mask<=127,"Invalid byte mutation bounds");
                require(bytes[index]>=32 && bytes[index]<127,"Opaque mutation supports ASCII byte only");
                int changed=bytes[index]^mask;require(changed>=32 && changed<127,"Mutation must preserve printable ASCII");bytes[index]=(byte)changed;
                return Json.MAPPER.valueToTree(new String(bytes,StandardCharsets.UTF_8));
            }
            case "jsonPointerReplace", "jsonPointerRemove": {
                require(source.isContainerNode() && source.toString().getBytes(StandardCharsets.UTF_8).length<=65536,"JSON mutation requires bounded actual JSON object/array result");
                require(t.size()==(op.equals("jsonPointerReplace")?4:3),"JSON mutation accepts source/operation/pointer/value only");
                String pointer=Json.required(t,"pointer");require(pointer.startsWith("/") && pointer.length()<=1024 && !pointer.matches(".*~([^01]|$).*"),"Mutation requires non-root RFC6901 pointer");
                int slash=pointer.lastIndexOf('/');JsonNode parent=source.at(pointer.substring(0,slash));String leaf=pointer.substring(slash+1).replace("~1","/").replace("~0","~");
                require(!source.at(pointer).isMissingNode(),"Mutation target missing");JsonNode value=t.get("value");
                if(op.equals("jsonPointerReplace")) require(value!=null && value.toString().getBytes(StandardCharsets.UTF_8).length<=65536 && !containsReference(value),"Replacement must be bounded literal input");
                if(parent.isObject()) {if(op.equals("jsonPointerRemove")) ((ObjectNode)parent).remove(leaf);else ((ObjectNode)parent).set(leaf,value.deepCopy());}
                else if(parent.isArray()) {require(leaf.matches("0|[1-9][0-9]{0,8}"),"Invalid array mutation index");int i=Integer.parseInt(leaf);var array=(com.fasterxml.jackson.databind.node.ArrayNode)parent;if(op.equals("jsonPointerRemove")) array.remove(i);else array.set(i,value.deepCopy());}
                else throw new IllegalArgumentException("Mutation parent is not observed container");return source;
            }
            default: throw new IllegalArgumentException("Unknown bounded transform operation "+op);
        }
    }
    private static boolean containsReference(JsonNode n){if(n.isObject() && (n.has("$result") || n.has("$alias") || n.has("$transform"))) return true;if(n.isContainerNode()) for(JsonNode child:n) if(containsReference(child)) return true;return false;}
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalArgumentException(reason);}
}

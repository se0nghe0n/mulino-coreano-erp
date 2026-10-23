package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import java.time.*;
import java.util.*;

/** Compiles authored facts plus a separately pinned input layer; additions never overwrite an authored value. */
final class ModelFixturePlan {
    record Row(String type,String alias,String id,String pointer,ObjectNode fields,boolean synthetic) {}
    final ObjectNode fixture,inputs,aliases;
    final List<Row> rows=new ArrayList<>();
    final ArrayNode issues=Json.array(),warnings=Json.array(),claims=Json.array(),authorityCorrections=Json.array(),purposeGrantBindings=Json.array(),inputCorrections=Json.array(),staticClaims=Json.array();
    final String org,hash;
    final Map<String,Row> byAlias=new LinkedHashMap<>();
    private final Set<String> usedRecords=new HashSet<>();
    ModelFixturePlan(JsonNode f,JsonNode layer,String hash) {
        fixture=(ObjectNode)f.deepCopy();inputs=(ObjectNode)layer.deepCopy();this.hash=hash;
        aliases=(ObjectNode)fixture.path("aliases").deepCopy();org=fixture.path("baseline").path("domainFacts").path("organizationId").asText();
    }
    void compile() {
        requireKeys(fixture,Set.of("schemaVersion","fixtureId","synthetic","baseRefs","clock","versions","actors","aliases","baseline","evidence","responsibilities"),"");
        requireKeys(fixture.path("baseline"),Set.of("domainFacts","principalFacts","grantFacts","sourceFixtureSha256"),"/baseline");
        if(!fixture.path("synthetic").asBoolean()||!fixture.path("baseRefs").isEmpty())problem("/synthetic","ISOLATED_SYNTHETIC_INPUT_REQUIRED");
        for(var i=aliases.fields();i.hasNext();){var e=i.next();try{UUID.fromString(e.getValue().asText());}catch(Exception invalid){problem("/aliases/"+e.getKey(),"UUID_ALIAS_REQUIRED");}}
        if(!aliases.path("org").asText().equals(org))problem("/baseline/domainFacts/organizationId","ORGANIZATION_ALIAS_MISMATCH");
        OffsetDateTime.parse(fixture.path("clock").path("asOf").asText());OffsetDateTime.parse(fixture.path("clock").path("knownAt").asText());
        for(JsonNode record:inputs.path("records")) {
            requireKeys(record,Set.of("type","alias","id","sourcePointer","part","fields","originalInputProvided","originalAuthorityClaimed","operatingApprovalClaimed","activate"),"/records");
            if(!ModelFixtureCatalog.KINDS.containsKey(record.path("type").asText()))problem("/records","UNKNOWN_FACT_KIND:"+record.path("type").asText());
            if(!record.path("fields").isObject())problem("/records","TYPED_FIELDS_REQUIRED");
            String a=record.path("alias").asText();String id=record.path("id").asText();
            if(a.isBlank()||id.isBlank()) {problem("/records","EXPLICIT_RECORD_ALIAS_AND_ID_REQUIRED");continue;}
            try{UUID.fromString(id);}catch(Exception invalid){problem("/records/"+a,"UUID_REQUIRED");}
            if(aliases.has(a)&&!aliases.path(a).asText().equals(id))problem("/records/"+a,"ORIGINAL_UUID_OVERRIDE_FORBIDDEN");
            else if(!aliases.has(a))aliases.put(a,id);
            if(record.path("sourcePointer").asText().isEmpty()&&fixture.path("aliases").has(a))problem("/records/"+a,"ORIGINAL_ALIAS_REQUIRES_SOURCE_POINTER");
            for(String flag:List.of("originalInputProvided","originalAuthorityClaimed","operatingApprovalClaimed"))if(!record.has(flag)||!record.path(flag).isBoolean()||record.path(flag).asBoolean())problem("/records/"+a+"/"+flag,"SYNTHETIC_PROVENANCE_FALSE_REQUIRED");
        }
        // Organization IDs are authored, while the external lookup label is only an installation locator.
        add("Organization","org","/baseline/domainFacts/organizationId","",Json.object().put("externalAlias",org));
        principals();grants();objects();sources();extras();
        for(JsonNode r:inputs.path("records"))if(r.path("sourcePointer").asText().isEmpty())add(r.path("type").asText(),r.path("alias").asText(),"","",Json.object());
        for(JsonNode r:inputs.path("records"))if(!usedRecords.contains(r.path("alias").asText()))problem("/records/"+r.path("alias").asText(),"UNCONSUMED_INPUT_RECORD");
        Set<String> ids=new HashSet<>();for(Row row:rows)if(!ids.add(row.id()))problem(row.pointer(),"DUPLICATE_RECORD_ID:"+row.id());
    }
    private void principals() {
        fixture.path("baseline").path("principalFacts").fields().forEachRemaining(e->{
            String p="/baseline/principalFacts/"+e.getKey();JsonNode f=e.getValue();
            requireKeys(f,Set.of("human","delegatorRef","roles","grantRef","subject","issuer","audience","organizationRef","readGrantRef","capabilities"),p);
            if(!f.path("organizationRef").asText().equals("org"))problem(p+"/organizationRef","CROSS_ORGANIZATION_PRINCIPAL");
            add("Principal",e.getKey(),p,"",Json.object().put("kind",f.path("human").asBoolean()?"HUMAN":"AGENT").put("stableRequestOwner",ref(e.getKey(),p)));
            var identity=Json.object().put("actorId",ref(e.getKey(),p)).put("issuer",f.path("issuer").asText()).put("subject",f.path("subject").asText()).put("organizationAlias",org);
            addGenerated("ExternalIdentity",e.getKey()+":identity",p,"identity",identity);
            if(f.has("capabilities")){claims.add(Json.object().put("sourcePointer",p+"/capabilities").set("originalClaim",f.path("capabilities")));}
            if(f.path("human").asBoolean())warnings.add(issue(p,"NO_UNSTATED_HUMAN_MEMBERSHIP_ROLE_OR_ROOT_GRANT_INSTALLED"));
        });
    }
    private void grants() {
        fixture.path("baseline").path("grantFacts").fields().forEachRemaining(e->{
            JsonNode g=e.getValue();String p="/baseline/grantFacts/"+e.getKey(),actor=g.path("actorRef").asText();
            requireKeys(g,Set.of("actorRef","organizationRef","actions","targetScope","validFrom","validUntil","revision","revokedAt","sourceNamespace","sourceNamespaces","delegatorRef"),p);
            if(!g.path("organizationRef").asText().equals("org"))problem(p+"/organizationRef","CROSS_ORGANIZATION_GRANT");
            String delegator=g.path("delegatorRef").asText(fixture.path("baseline").path("principalFacts").path(actor).path("delegatorRef").asText());
            var fields=Json.object().put("actorId",ref(actor,p)).put("delegatorId",ref(delegator,p));
            for(String k:List.of("validFrom","validUntil","revision","revokedAt"))if(g.has(k))fields.set(k,g.path(k));
            add("Grant",e.getKey(),p,"",fields);
            var membership=Json.object().put("actorId",ref(actor,p));for(String k:List.of("validFrom","validUntil"))membership.set(k,g.path(k));
            addGenerated("Membership",e.getKey()+":membership",p,"membership",membership);
            // Exact role strings remain exact: WRITE never becomes every domain capability.
            for(JsonNode role:fixture.path("baseline").path("principalFacts").path(actor).path("roles")) {
                var assignment=membership.deepCopy().put("capabilityId",role.asText()).put("scopeKind","ORGANIZATION").put("scopeId",org);
                addGenerated("CapabilityAssignment",e.getKey()+":role:"+role.asText(),p,"role",assignment);
            }
        });
    }
    private void objects() {
        fixture.path("baseline").path("domainFacts").path("objects").fields().forEachRemaining(e->{
            String a=e.getKey(),p="/baseline/domainFacts/objects/"+a;JsonNode f=e.getValue();String t=f.path("type").asText();var out=Json.object();
            if(a.equals("A")&&fixture.path("fixtureId").asText().equals("M50-isolated")&&inputs.path("physicalCorrection").path("version").asText().equals("model-M50-original-reference-only-v1")){claims.add(Json.object().put("sourcePointer",p).put("status","ORIGINAL_REFERENCE_ONLY").put("originalIdentityPreservedInProduct",false).set("originalSnapshot",f.deepCopy()));warnings.add(issue(p,"ORIGINAL_UNCHANGED_CASE_BLOCKED_PHYSICAL_IDENTITY_CONFLICT"));return;}
            switch(t) {
                case "TradeItem" -> {copy(f,out,"name","baseUnit");map(f,out,"scale","decimalPlaces",false,p);
                    requireKeys(f,Set.of("type","name","baseUnit","scale","issuer","sku","conversion","ref"),p);
                    if(f.has("issuer"))requirePart(p,"manufacturer","Manufacturer");
                    if(f.has("issuer")||f.has("sku"))addGenerated("ExternalIdentifier",a+":identifier",p,"identifier",Json.object().put("itemId",ref(a,p)).put("issuer",ref(f.path("issuer").asText(),p)).put("value",f.path("sku").asText()));
                    f.path("conversion").fields().forEachRemaining(c->{String[] units=c.getKey().split("_to_",-1);if(units.length!=2){problem(p+"/conversion","INVALID_CONVERSION_KEY");return;}
                        addGenerated("UnitConversion",a+":conversion:"+c.getKey(),p,"conversion:"+c.getKey(),Json.object().put("itemId",ref(a,p)).put("fromUnit",units[0]).put("toUnit",units[1]).set("factor",c.getValue()));});}
                case "Place" -> {copy(f,out,"name");requireKeys(f,Set.of("type","name","organizationRef","ref"),p);if(!f.path("organizationRef").asText().equals("org"))problem(p+"/organizationRef","CROSS_ORGANIZATION_PLACE");}
                case "Customer" -> {copy(f,out,"name");requireKeys(f,Set.of("type","name","ref"),p);}
                case "ManufacturerLot" -> {map(f,out,"tradeItemRef","itemId",true,p);map(f,out,"manufacturerRef","manufacturerId",true,p);map(f,out,"externalLabel","originalLot",false,p);requireKeys(f,Set.of("type","tradeItemRef","manufacturerRef","externalLabel","ref"),p);}
                case "QuantitySegment" -> {
                    map(f,out,"tradeItemRef","itemId",true,p);map(f,out,"lotRef","lotId",true,p);map(f,out,"placeRef","placeId",true,p);copy(f,out,"quantity","unit","revision");out.put("identificationStatus","CONFIRMED");
                    requireKeys(f,Set.of("type","tradeItemRef","lotRef","quantity","unit","placeRef","active","revision","qc","regulatory","disposition","allocated","returnRef","ref"),p);
                    if(f.path("active").asBoolean())out.putNull("retiredAt").putNull("retirementRecordedAt");else {JsonNode retirement=findRecord(p,"", "QuantitySegment");if(retirement==null||!retirement.path("fields").hasNonNull("retiredAt")||!retirement.path("fields").hasNonNull("retirementRecordedAt"))problem(p+"/retirement","EXPLICIT_RETIREMENT_CLOCKS_REQUIRED");}
                    for(String condition:List.of("qc","regulatory","disposition"))if(f.has(condition)&&!f.path(condition).asText().equals("UNKNOWN"))requirePart(p,condition,condition.equals("regulatory")?"RegulatoryDecision":"DispositionBasis or Restriction with an explicit decision/evidence");
                    if(f.has("returnRef"))requirePart(p,"return","ReturnReceipt");
                    if(!f.path("allocated").asText("0").equals("0"))requirePart(p,"allocation","Allocation");
                }
                case "Work" -> {
                    map(f,out,"state","status",false,p);map(f,out,"ownerRef","ownerId",true,p);copy(f,out,"revision");definitionRef(f,out,p);
                    requireKeys(f,Set.of("type","state","ownerRef","definitionVersion","goal","revision","ref"),p);
                    var goal=Json.object().put("workId",ref(a,p));if(out.has("definitionVersionId"))goal.set("definitionVersionId",out.path("definitionVersionId"));
                    map(f.path("goal"),goal,"quantity","targetQuantity",false,p);copy(f.path("goal"),goal,"unit","endpoint","dueAt");
                    String mode=f.path("goal").path("quantityMode").asText();goal.put("quantityMode",mode.equals("CUMULATIVE")?"CUMULATIVE_EVENT":mode);
                    addGenerated("Goal",a+":goal",p,"goal",goal);
                    requireKeys(f.path("goal"),Set.of("quantity","unit","endpoint","quantityMode","destinationRef","dueAt","dueInclusive"),p+"/goal");
                }
                case "SalesOrder" -> {
                    copy(f,out,"revision");out.put("currentRevision",f.path("revision").asInt());
                    requireKeys(f,Set.of("type","customerRef","itemRef","quantity","unit","endpoint","ownerRef","revision","ref","goal","deliveryContributionRefs"),p);
                    var line=Json.object().put("orderId",ref(a,p));map(f,line,"customerRef","customerId",true,p);map(f,line,"itemRef","itemId",true,p);copy(f,line,"quantity","unit");map(f,line,"endpoint","deliveryEndpoint",false,p);
                    addGenerated("SalesLine",a+":line",p,"line",line);requirePart(p,"work","Work");requirePart(p,"revision","SalesRevision");
                    if(f.has("goal"))requirePart(p,"goal","Goal");if(f.has("deliveryContributionRefs"))requirePart(p,"contributions","immutable source contributions");
                }
                default -> problem(p+"/type","UNKNOWN_OBJECT_TYPE:"+t);
            }
            if(ModelFixtureCatalog.KINDS.containsKey(t))add(t,a,p,"",out);
        });
        JsonNode d=fixture.path("baseline").path("domainFacts").path("definition");
        requireKeys(d,Set.of("activeVersion","evaluatorVersion","schemaVersion","supportedVersions","goalEndpoint","quantityMode","goalTemplateDefaults","v2Endpoint"),"/baseline/domainFacts/definition");
        requireKeys(d.path("goalTemplateDefaults"),Set.of("approvedByRef","dueInclusive","quantityMode","sourceRef","syntheticDefinitionApproval"),"/baseline/domainFacts/definition/goalTemplateDefaults");
        requireKeys(fixture.path("baseline").path("domainFacts").path("policy"),Set.of("version","synthetic","operationalAuthority","quantityTolerance","purchaseApproval","qcDecision","recallDecision","stockAdjustment","reservationAdditionalHumanApproval","sourceEvidence","intakeOwnerRef","supervisorRef","currentSell","recallHold"),"/baseline/domainFacts/policy");
        if(!d.isMissingNode())requirePart("/baseline/domainFacts/definition","","DefinitionVersion with complete typed content; metadata is insufficient");
        if(fixture.path("baseline").path("domainFacts").has("policy"))requirePart("/baseline/domainFacts/policy","","PolicyVersion with complete current rule content; summary is insufficient");
        JsonNode originalPolicy=fixture.at("/baseline/domainFacts/policy");for(String part:List.of("currentSell","recallHold"))if(originalPolicy.hasNonNull(part)) {
            if(!originalPolicy.path(part).asText().equals(part.equals("currentSell")?"DENIED":"ACTIVE")){problem("/baseline/domainFacts/policy/"+part,"UNKNOWN_CURRENT_POLICY_FACT");continue;}
            boolean mapped=false;for(JsonNode record:inputs.path("records"))if(record.path("sourcePointer").asText().equals("/baseline/domainFacts/policy")&&record.path("part").asText().startsWith(part+":")) {
                if(!record.path("type").asText().equals("Restriction"))problem("/baseline/domainFacts/policy/"+part,"EXPLICIT_CURRENT_RESTRICTION_REQUIRED");else {add("Restriction",record.path("alias").asText(),"/baseline/domainFacts/policy",record.path("part").asText(),Json.object().put("state","ACTIVE").put("category",part.equals("currentSell")?"COMMERCIAL":"RECALL_INVESTIGATION"));mapped=true;}
            }if(!mapped)problem("/baseline/domainFacts/policy/"+part,"CURRENT_POLICY_RESTRICTION_NOT_INSTALLED");
        }

        int n=0;for(JsonNode o:fixture.path("baseline").path("domainFacts").path("obligations")) {
            String p="/baseline/domainFacts/obligations/"+n++;var fields=Json.object();copy(o,fields,"kind","status","nextAction","nextCheckAt");map(o,fields,"ownerRef","ownerId",true,p);
            if(o.has("quantity")){fields.set("quantity",o.path("quantity").path("value"));fields.set("unit",o.path("quantity").path("unit"));}
            requireKeys(o,Set.of("kind","scopeRef","sourceRef","rootRef","ownerRef","status","nextAction","nextCheckAt","quantity"),p);
            addGenerated("Obligation","obligation:"+(n-1),p,"",fields);
        }
    }
    private void sources() {
        fixture.path("baseline").path("domainFacts").path("sources").fields().forEachRemaining(e->{String p="/baseline/domainFacts/sources/"+e.getKey();
            requireKeys(e.getValue(),Set.of("authenticated","issuer","organizationRef","canonicalOccurrenceRef","version","trust","payloadHash"),p);
            addGenerated("SourceProfile","source:"+e.getKey(),p,"",Json.object().put("namespace",e.getKey()));
            warnings.add(issue(p,"AUTHENTICATED_TRUST_AND_PAYLOAD_HASH_ARE_UNPROVEN_SOURCE_CLAIMS"));
            claims.add(Json.object().put("sourcePointer",p).set("originalClaim",e.getValue()));
        });
        fixture.path("baseline").path("domainFacts").path("evidence").fields().forEachRemaining(e->{String p="/baseline/domainFacts/evidence/"+e.getKey();JsonNode f=e.getValue();
            requireKeys(f,Set.of("sha256","available","sourceNamespace","eventRef"),p);
            var fields=Json.object();copy(f,fields,"sha256","sourceNamespace");fields.put("availability","UNKNOWN");
            add("Document",e.getKey(),p,"",fields);
            if(f.path("available").asBoolean())warnings.add(issue(p+"/available","ORIGINAL_AVAILABILITY_UNVERIFIABLE_WITHOUT_MATCHING_ORIGINAL_BYTES"));
            claims.add(Json.object().put("sourcePointer",p).set("originalClaim",f));
        });
    }
    private void extras() {
        Set<String> common=Set.of("organizationId","objects","sources","evidence","definition","policy","obligations","approvalFacts","baselineEffects");
        fixture.path("baseline").path("domainFacts").fields().forEachRemaining(e->{
            if(common.contains(e.getKey()))return;
            if(staticClaim(e.getKey(),e.getValue()))return;
            String pointer="/baseline/domainFacts/"+e.getKey();
            if(Set.of("receipt","transport").contains(e.getKey())&&fixture.path("fixtureId").asText().equals("M50-isolated")&&inputs.path("priorReceipt").path("version").asText().equals("model-M50-prior-receipt-v1")){claims.add(Json.object().put("sourcePointer",pointer).put("status","ORIGINAL_REFERENCE_ONLY_ACTUAL_PRIOR_COMMAND_REQUIRED").set("originalClaim",e.getValue()));return;}
            if(e.getKey().equals("receipt")&&fixture.path("fixtureId").asText().equals("M03-isolated")) {requireKeys(e.getValue(),Set.of("alreadyReceived","observed","sourceNamespace","canonicalOccurrenceRef","confirmedIdentity","transportPhysicalExists"),pointer);JsonNode record=findRecord(pointer,"","ReceiptObservation");if(record==null)problem(pointer,"EXPLICIT_INCOMING_RECEIPT_REQUIRED");else add("ReceiptObservation",record.path("alias").asText(),pointer,"",Json.object().put("quantity",e.getValue().path("observed").asText()).put("state","PROVISIONAL"));return;}
            if(e.getKey().equals("holds")&&fixture.path("fixtureId").asText().equals("M42-isolated")) {
                int index=0;for(JsonNode h:e.getValue()){String hp=pointer+"/"+index++;requireKeys(h,Set.of("ref","scopeRef","kind","quantity","unit","status"),hp);String alias=h.path("ref").asText();Row existing=byAlias.get(alias);if(existing==null||!existing.type().equals("Restriction")||!existing.fields().path("category").equals(h.path("kind"))||!existing.fields().path("quantity").equals(h.path("quantity"))||!existing.fields().path("unit").equals(h.path("unit"))||!existing.fields().path("state").equals(h.path("status")))problem(hp,"EXACT_ORIGINAL_HOLD_SOURCE_MAPPING_REQUIRED");else claims.add(Json.object().put("sourcePointer",hp).put("installedFactAlias",alias).set("originalClaim",h.deepCopy()));}return;
            }
            if(e.getKey().equals("c2")) {
                requireKeys(e.getValue(),Set.of("mondayArrived","tuesdayDispatched","wednesdayArrived","onHand","arrived"),pointer);
                for(String part:List.of("mondayArrived","wednesdayArrived","tuesdayDispatched")){String type=part.equals("tuesdayDispatched")?"Movement":"Receipt";JsonNode record=findRecord(pointer,part,type);if(record==null)problem(pointer+"/"+part,"EXPLICIT_C2_SOURCE_CHAIN_REQUIRED");else add(type,record.path("alias").asText(),pointer,part,Json.object());}return;
            }
            if(Set.of("supplierReply","shipment","regulatory","invoice","proposal").contains(e.getKey())&&selectedCaseFact(e.getKey(),e.getValue(),pointer))return;
            if(e.getKey().equals("deliveryReport")&&fixture.path("fixtureId").asText().equals("M51-isolated")) {
                requireKeys(e.getValue(),Set.of("quantity","stillTransport","sourceNamespace","verifiedPhysicalScope"),pointer);JsonNode record=findRecord(pointer,"","CanonicalOccurrence");if(record==null)problem(pointer,"EXPLICIT_DELIVERY_REPORT_SOURCE_REQUIRED");else add("CanonicalOccurrence",record.path("alias").asText(),pointer,"",Json.object().put("quantity",e.getValue().path("quantity").asText()));return;
            }
            if(e.getKey().equals("inventory")){requireKeys(e.getValue(),Set.of("consignmentSell","customerCustody","onHand","reserved"),pointer);return;}
            String type=switch(e.getKey()) {
                case "allocation" -> "Allocation";case "dispatch" -> "Dispatch";default -> null;
            };
            if(type!=null&&!(e.getKey().equals("allocation")&&e.getValue().path("status").asText().equals("NONE"))) {
                var fields=Json.object();copy(e.getValue(),fields,"quantity","unit");if(type.equals("Allocation"))map(e.getValue(),fields,"status","state",false,pointer);
                map(e.getValue(),fields,"scopeRef","segmentId",true,pointer);
                if(e.getValue().hasNonNull("orderRef")){JsonNode typed=inputs.path("typedReferences").path(e.getValue().path("orderRef").asText());if(!typed.hasNonNull("SalesOrderLine"))problem(pointer+"/orderRef","TYPED_SALES_LINE_BINDING_REQUIRED");else fields.set("orderLineId",typed.path("SalesOrderLine"));}map(e.getValue(),fields,"allocationRef","allocationId",true,pointer);map(e.getValue(),fields,"customerRef","customerId",true,pointer);
                if(type.equals("Dispatch")&&e.getValue().has("allocationStatus")){JsonNode a=findRecord(pointer,"allocationStatus","Allocation");if(a==null)problem(pointer,"DISPATCH_ALLOCATION_HISTORY_REQUIRED");else add("Allocation",a.path("alias").asText(),pointer,"allocationStatus",Json.object().put("state",e.getValue().path("allocationStatus").asText()));}
                String alias=e.getValue().path("ref").asText();if(alias.isBlank())alias=e.getKey()+":baseline";
                add(type,alias,pointer,"",fields);
                // Remaining attributes are claims until backed by an explicit physical chain.
                Set<String> base=type.equals("Dispatch")?Set.of("ref","quantity","unit","allocationRef","customerRef","transportRemaining","allocationStatus","verified","cargoScope"):Set.of("ref","quantity","unit","status","scopeRef","allocationRef","customerRef","orderRef");
                for(var keys=e.getValue().fieldNames();keys.hasNext();){String k=keys.next();if(!base.contains(k))requirePart(pointer,k,"typed physical source facts");}
            } else if(e.getKey().equals("allocation")&&e.getValue().path("status").asText().equals("NONE")) {
                claims.add(Json.object().put("sourcePointer",pointer).put("installationCheck","NO_ALLOCATION_ROWS"));
            } else requirePart(pointer,"","explicit typed source graph; no summary/expected Boolean seed");
        });
        if(!fixture.path("baseline").path("domainFacts").path("approvalFacts").isEmpty())requirePart("/baseline/domainFacts/approvalFacts","","typed immutable approvals");
        JsonNode effects=fixture.path("baseline").path("domainFacts").path("baselineEffects");
        requireKeys(effects,Set.of("bankTransferCount","externalSubmissionCount","newStockCount"),"/baseline/domainFacts/baselineEffects");
        effects.fields().forEachRemaining(e->{if(!e.getValue().isIntegralNumber()||e.getValue().asLong()!=0)problem("/baseline/domainFacts/baselineEffects/"+e.getKey(),"EXTERNAL_OR_DERIVED_EFFECT_SEED_FORBIDDEN");});
        if(!fixture.path("evidence").isEmpty())problem("/evidence","UNMAPPED_TOP_LEVEL_EVIDENCE");
        // Responsibilities duplicate the declared duties; equality is checked rather than independently seeded.
        for(JsonNode responsibility:fixture.path("responsibilities")) {
            boolean match=false;for(JsonNode o:fixture.path("baseline").path("domainFacts").path("obligations"))if(o.path("kind").equals(responsibility.path("scope").path("kind"))&&o.path("ownerRef").equals(responsibility.path("ownerAlias"))&&o.path("nextAction").equals(responsibility.path("nextAction"))&&o.path("nextCheckAt").equals(responsibility.path("nextCheckAt")))match=true;
            if(!match)problem("/responsibilities","RESPONSIBILITY_HAS_NO_EXACT_ORIGINAL_OBLIGATION");
        }
    }
    private void requirePart(String pointer,String part,String need) {
        JsonNode match=null;for(JsonNode r:inputs.path("records"))if(r.path("sourcePointer").asText().equals(pointer)&&r.path("part").asText().equals(part)){if(match!=null)problem(pointer,"DUPLICATE_SOURCE_BINDING:"+part);match=r;}
        if(match==null){problem(pointer+(part.isEmpty()?"":"/"+part),"EXPLICIT_INPUT_REQUIRED:"+need);return;}
        String expected=switch(need) {
            case "Work","SalesRevision","Goal","ReturnReceipt","Allocation","Manufacturer","RegulatoryDecision" -> need;
            case "DefinitionVersion with complete typed content; metadata is insufficient" -> "DefinitionVersion";
            case "PolicyVersion with complete current rule content; summary is insufficient" -> "PolicyVersion";
            case "DispositionBasis or Restriction with an explicit decision/evidence" -> "QUALITY_SOURCE_GRAPH";
            default -> "UNRESOLVED_SOURCE_GRAPH";
        };
        String actual=match.path("type").asText();
        if(expected.equals("QUALITY_SOURCE_GRAPH")) {
            JsonNode original=fixture.at(pointer);String value=original.path(part).asText();
            String selected=part.equals("qc")?(value.equals("PASS")?"DispositionBasis":"Restriction"):(value.equals("CUSTOMER_CUSTODY_SELL_DENIED")?"Restriction":"DispositionBasis");
            if(!selected.equals(actual)){problem(pointer+"/"+part,"QUALITY_BINDING_KIND_MISMATCH:"+selected);return;}
            ObjectNode facts=Json.object().put("segmentId",ref(pointer.substring(pointer.lastIndexOf('/')+1),pointer)).put("quantity",original.path("quantity").asText()).put("unit",original.path("unit").asText()).put("category",part.equals("qc")?"QC":"COMMERCIAL").put("action","SELL").put("state","ACTIVE");
            add(actual,match.path("alias").asText(),pointer,part,facts);return;
        }
        if(expected.equals("UNRESOLVED_SOURCE_GRAPH")) {
            problem(pointer+(part.isEmpty()?"":"/"+part),"UNRESOLVED_TYPED_SOURCE_GRAPH:"+need);return;
        }
        if(!expected.equals(actual)){problem(pointer,"SOURCE_BINDING_KIND_MISMATCH:"+expected);return;}
        if(expected.equals("RegulatoryDecision")) {
            JsonNode original=fixture.at(pointer);String alias=pointer.substring(pointer.lastIndexOf('/')+1);
            add(actual,match.path("alias").asText(),pointer,part,Json.object().put("physicalScopeId",ref(alias,pointer)).put("itemId",ref(original.path("tradeItemRef").asText(),pointer)).put("lotId",ref(original.path("lotRef").asText(),pointer)).put("quantity",original.path("quantity").asText()).put("unit",original.path("unit").asText()).put("action","SELL").put("decision",original.path("regulatory").asText()));return;
        }
        add(actual,match.path("alias").asText(),pointer,part,Json.object());
    }
    private boolean selectedCaseFact(String key,JsonNode original,String pointer) {
        String caseId=fixture.path("fixtureId").asText();String type;
        Set<String> allowed;
        switch(key) {
            case "supplierReply"->{if(!caseId.equals("M04-isolated"))return false;type="CanonicalOccurrence";allowed=Set.of("sourceNamespace","acceptedQuantity","verified");}
            case "shipment"->{if(!caseId.equals("M05-isolated"))return false;type="Shipment";allowed=Set.of("ref","plannedDestinationRef","lastConfirmedPlace","cargo","unit");}
            case "regulatory"->{if(!caseId.equals("M07-isolated"))return false;type="RegulatoryProcedure";allowed=Set.of("procedureRef","authorityReceipt","submittedAt","receiptVerified");}
            case "invoice"->{if(!caseId.equals("M14-isolated"))return false;type="Invoice";allowed=Set.of("ref","currency","amount");}
            case "proposal"->{if(!caseId.equals("M45-isolated"))return false;type="PurchaseProposal";allowed=Set.of("revision","quantity","hash","approvedBy","approvalHash","canonicalPayload","approvalRevision","approvalScope","approvalValidUntil");}
            default->{return false;}
        }
        requireKeys(original,allowed,pointer);JsonNode record=findRecord(pointer,"",type);if(record==null){problem(pointer,"EXPLICIT_SELECTED_TYPED_INPUT_REQUIRED:"+type);return true;}
        ObjectNode fields=Json.object();if(type.equals("Invoice"))fields.put("currency",original.path("currency").asText()).put("originalAmount",original.path("amount").asText());
        if(type.equals("CanonicalOccurrence"))fields.put("quantity",original.path("acceptedQuantity").asText()).put("kind","SUPPLIER_ACCEPT");
        if(type.equals("Shipment"))fields.put("destinationId",ref(original.path("plannedDestinationRef").asText(),pointer));
        if(type.equals("PurchaseProposal"))fields.put("currentRevision",original.path("revision").asInt());
        add(type,record.path("alias").asText(),pointer,"",fields);claims.add(Json.object().put("sourcePointer",pointer).put("originalAuthorityClaimed",false).set("originalClaim",original.deepCopy()));return true;
    }
    private JsonNode findRecord(String pointer,String part,String type) {for(JsonNode r:inputs.path("records"))if(r.path("sourcePointer").asText().equals(pointer)&&r.path("part").asText().equals(part)&&r.path("type").asText().equals(type))return r;return null;}
    private boolean staticClaim(String key,JsonNode original) {
        if(!Set.of("fx","selectionContext","selectedRefs","documentContent","actualLocationStatus","recallCandidates","contaminationConfirmed").contains(key))return false;
        JsonNode claim=inputs.path("staticClaims").path(key);if(!claim.path("originalClaim").equals(original)||!claim.path("sourcePointer").asText().equals("/baseline/domainFacts/"+key)||!claim.path("sourceClass").asText().equals("ORIGINAL_STATIC_CLAIM_UNVERIFIED")){problem("/baseline/domainFacts/"+key,"EXPLICIT_STATIC_CLAIM_RECEIPT_REQUIRED");return true;}
        staticClaims.add(claim.deepCopy());warnings.add(issue("/baseline/domainFacts/"+key,"STATIC_CLAIM_NOT_CURRENT_PRODUCT_FACT_OR_PROMPT_INPUT"));return true;
    }
    private void definitionRef(JsonNode f,ObjectNode fields,String p) {
        String v=f.path("definitionVersion").asText();String found=null;
        for(JsonNode r:inputs.path("records"))if(r.path("type").asText().equals("DefinitionVersion")&&r.path("fields").path("version").asText().equals(v)){if(found!=null)problem(p,"AMBIGUOUS_DEFINITION_VERSION");found=r.path("id").asText();}
        if(found==null)problem(p+"/definitionVersion","COMPLETE_DEFINITION_CONTENT_REQUIRED:"+v);else fields.put("definitionVersionId",found);
    }
    private void addGenerated(String type,String alias,String pointer,String part,ObjectNode fields) {
        if(!aliases.has(alias))aliases.put(alias,UUID.nameUUIDFromBytes((hash+":"+type+":"+alias).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
        add(type,alias,pointer,part,fields);
    }
    private void add(String type,String alias,String pointer,String part,ObjectNode authored) {
        ModelFixtureCatalog.Kind kind=ModelFixtureCatalog.KINDS.get(type);if(kind==null){problem(pointer,"UNKNOWN_FACT_KIND:"+type);return;}
        JsonNode supplement=null;
        for(JsonNode r:inputs.path("records"))if(r.path("alias").asText().equals(alias)||(r.path("sourcePointer").asText().equals(pointer)&&!pointer.isEmpty()&&r.path("part").asText().equals(part))){
            if(supplement!=null&&!supplement.equals(r)){problem(pointer,"AMBIGUOUS_RECORD_BINDING:"+part);return;}supplement=r;
        }
        ObjectNode fields=authored.deepCopy();String id=aliases.path(alias).asText();
        if(supplement!=null) {
            if(!supplement.path("type").asText().equals(type)){problem(pointer,"RECORD_TYPE_MISMATCH:"+type);return;}
            alias=supplement.path("alias").asText();id=supplement.path("id").asText();usedRecords.add(alias);
            for(var i=supplement.path("fields").fields();i.hasNext();){var e=i.next();if(fields.has(e.getKey())&&!same(fields.path(e.getKey()),e.getValue()))problem(pointer+"/"+e.getKey(),"ORIGINAL_VALUE_OVERRIDE_FORBIDDEN");else fields.set(e.getKey(),e.getValue());}
        }
        if(id.isBlank()){problem(pointer,"EXPLICIT_SUPPORT_ID_REQUIRED:"+type+":"+part);return;}
        if(byAlias.containsKey(alias)){if(!byAlias.get(alias).fields().equals(fields))problem(pointer,"DUPLICATE_ALIAS_BINDING:"+alias);return;}
        fields.putIfAbsent("revision",Json.MAPPER.valueToTree(1));if(kind.fields().containsKey("createdAt"))fields.putIfAbsent("createdAt",fixture.path("clock").path("asOf"));if(kind.temporal())fields.putIfAbsent("recordedAt",fixture.path("clock").path("asOf"));
        if(kind.fields().containsKey("effectiveAt"))fields.putIfAbsent("effectiveAt",fixture.path("clock").path("asOf"));
        for(String k:kind.required())if(!fields.hasNonNull(k))problem(pointer+"/"+k,"REQUIRED_TYPED_FIELD:"+type);
        requireKeys(fields,kind.fields().keySet(),pointer);
        Row row=new Row(type,alias,id,pointer,fields,supplement!=null||!fixture.path("aliases").has(alias)||pointer.contains("synthetic-manager-root:"));rows.add(row);byAlias.put(alias,row);
    }
    private boolean same(JsonNode a,JsonNode b){if(a.equals(b))return true;return a.isTextual()&&b.isTextual()&&aliases.path(b.asText()).asText().equals(a.asText());}
    private void map(JsonNode source,ObjectNode into,String from,String to,boolean reference,String p){if(source.hasNonNull(from)){if(reference)into.put(to,ref(source.path(from).asText(),p+"/"+from));else into.set(to,source.path(from));}}
    private static void copy(JsonNode from,ObjectNode to,String... keys){for(String k:keys)if(from.has(k))to.set(k,from.path(k));}
    String ref(String alias,String p){if(!aliases.hasNonNull(alias)){problem(p,"UNKNOWN_ALIAS:"+alias);return "";}return aliases.path(alias).asText();}
    void problem(String pointer,String reason){issues.add(issue(pointer,reason));}
    static ObjectNode issue(String pointer,String reason){return Json.object().put("pointer",pointer).put("reason",reason);}
    private void requireKeys(JsonNode node,Set<String> keys,String p){node.fieldNames().forEachRemaining(k->{if(!keys.contains(k))problem(p+"/"+k,"UNKNOWN_FACT_FIELD");});}
}

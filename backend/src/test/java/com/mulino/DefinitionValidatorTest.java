package com.mulino;

import com.mulino.domain.definitions.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.*;

class DefinitionValidatorTest {
 final DefinitionValidator validator=new DefinitionValidator();
 static Definition fixture() {
   return new Definition("00000000-0000-0000-0000-000000000001","00000000-0000-0000-0000-000000000010","definition-v1",null,"PUBLISHED","","core-v1","1.0.0",
     List.of(new Definition.NounType("QuantitySegment",true),new Definition.NounType("Place",true)),
     List.of(new Definition.Attribute("QuantitySegment","quantity",Definition.ValueType.DECIMAL,null,"BOX",0,1,1,"CONFIRM_RECEIPT",true),
       new Definition.Attribute("QuantitySegment","destination",Definition.ValueType.REFERENCE,"Place",null,0,1,1,"DRAFT",true)),
     List.of(new Definition.Verb("getDefinition","QUERY","getDefinition","READ",Map.of())),
     List.of(new Definition.Relation("locatedAt","QuantitySegment","Place",0,1,false)),
     List.of(new Definition.Goal("arrived","CUMULATIVE_EVENT","ARRIVED","core-v1",Map.of("operator","exists","property","QuantitySegment.quantity"))),
     List.of(new Definition.Capability("getDefinition","core-v1","core-v1","1.0.0","1.0.0",List.of())));
 }
 @Test void storedFixtureHashAndSemanticManifestMatch() throws Exception {
   String content=java.nio.file.Files.readString(java.nio.file.Path.of("../docs/execution/s1-definitions/definition-v1.json"));
   var json=new com.fasterxml.jackson.databind.ObjectMapper();
   var d=json.readValue(content,Definition.class);
   var manifest=json.readTree(java.nio.file.Files.readString(java.nio.file.Path.of("../docs/execution/s1-definitions/definition-manifest.json")));
   assertEquals(manifest.get("sha256").asText(),DefinitionRepository.sha256(content));
   assertEquals("VALID",validator.validate(d).outcome());
   assertEquals("HELD_UNSUPPORTED",validator.compatibility(d,"resumeWork","eligibility-v2","2.0.0","2.0.0").outcome());
 }
 @Test void stageAndReservedExtensionFailClosed() {
   var d=fixture();
   assertEquals("INVALID",validator.validateIntent(d,"getDefinition","COMMAND","READ",Map.of()).outcome());
   var extension=new Definition.Attribute("QuantitySegment","eligible",Definition.ValueType.BOOLEAN,null,null,0,0,1,"READ",false);
   var changed=new Definition(d.organizationId(),d.id(),d.version(),null,d.state(),d.contentHash(),d.evaluatorVersion(),d.schemaVersion(),d.nouns(),List.of(extension),d.verbs(),d.relations(),List.of(),d.capabilities());
   assertTrue(validator.validate(changed).problems().stream().anyMatch(p->p.code().equals("RESERVED_CORE")));
 }
 @Test void publishedFixtureHasValidTypesCardinalityAndManifest() {assertEquals("VALID",validator.validate(fixture()).outcome());}
 @Test void datesCannotBePlacesAndCardinalityIsEnforced() {
   var d=fixture();assertEquals("INVALID",validator.validateValue(d.attributes().get(1),"2026-10-07").outcome());
   assertEquals("INVALID",validator.validateRelation(d.relations().getFirst(),"QuantitySegment",List.of("Place","Place")).outcome());
   assertEquals("INVALID",validator.validateRelation(d.relations().getFirst(),"Place",List.of("QuantitySegment")).outcome());
 }
 @Test void decimalRequiresExactStringCompatibleUnitAndNoRounding() {
   var a=fixture().attributes().getFirst();
   assertEquals("VALID",validator.validateValue(a,Map.of("value","100","unit","BOX")).outcome());
   for(var v:List.of(Map.of("value",100,"unit","BOX"),Map.of("value","1.2","unit","BOX"),Map.of("value","1","unit","EA"),Map.of("value","100000000000000000000000000","unit","BOX"))) assertEquals("INVALID",validator.validateValue(a,v).outcome());
 }
 @Test void injectedOperatorsAndPropertiesFailClosed() {
   assertEquals("UNSUPPORTED",validator.validatePredicate(fixture(),Map.of("operator","sql","value","SELECT 1")).outcome());
   assertEquals("INVALID",validator.validatePredicate(fixture(),Map.of("operator","exists","property","QuantitySegment.role")).outcome());
   assertEquals("UNSUPPORTED",validator.validatePredicate(fixture(),Map.of("operator","exists","property","QuantitySegment.quantity","script","eval()")).outcome());
   assertEquals("UNSUPPORTED",validator.validatePredicate(fixture(),Map.of("operator","exists","property","QuantitySegment.quantity","children",List.of(Map.of("operator","sql")))).outcome());
 }
 @Test void recursivePredicateAndOperandSizesAreBounded() {
   assertTrue(validator.validatePredicate(fixture(),Map.of("operator","range","property","QuantitySegment.quantity","minimum",Map.of("value","20","unit","BOX"),"maximum",Map.of("value","10","unit","BOX"))).problems().stream().anyMatch(p->p.code().equals("REVERSED_BOUNDS")));
   Map<String,Object> node=Map.of("operator","exists","property","QuantitySegment.quantity");
   for(int i=0;i<34;i++) node=Map.of("operator","not","children",List.of(node));
   assertTrue(validator.validatePredicate(fixture(),node).problems().stream().anyMatch(p->p.code().equals("DEPTH_LIMIT")));
   var children=Collections.nCopies(101,Map.of("operator","exists","property","QuantitySegment.quantity"));
   assertEquals("INVALID",validator.validatePredicate(fixture(),Map.of("operator","all","children",children)).outcome());
 }
 @Test void unknownAndConflictSurviveLogicalOperations() {
   var unknown=new PredicateTruth(PredicateTruth.State.UNVERIFIED,true);
   var yes=new PredicateTruth(PredicateTruth.State.SATISFIED,false);
   var no=new PredicateTruth(PredicateTruth.State.UNSATISFIED,false);
   assertEquals(new PredicateTruth(PredicateTruth.State.UNSATISFIED,true),PredicateTruth.all(List.of(no,unknown)));
   assertEquals(new PredicateTruth(PredicateTruth.State.SATISFIED,true),PredicateTruth.any(List.of(yes,unknown)));
   assertEquals(unknown,unknown.not());
 }
}

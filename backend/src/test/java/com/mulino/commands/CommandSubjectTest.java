package com.mulino.commands;
import com.mulino.application.core.*;
import com.mulino.domain.definitions.*;
import com.sap.cds.Result;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CommandSubjectTest {
 final String org=UUID.randomUUID().toString(),actor=UUID.randomUUID().toString(),work=UUID.randomUUID().toString(),other=UUID.randomUUID().toString(),definitionId=UUID.randomUUID().toString();
 final DomainContext context=new DomainContext(org,actor,actor,Instant.now(),Instant.now());
 final CommandPreparation prep=CommandPreparation.ordinary(Map.of("WORK",List.of(work,other)),List.of(),"INTERNAL_MOVE",work,0);
 final CommandHandler handler=new CommandHandler(){
  public Set<String> capabilities(){return Set.of("closeWork");}
  public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){throw new AssertionError("Effect must not run during subject verification");}
  public List<SubjectBinding> subjectBindings(DomainContext c,Map<String,Object> intent,CommandPreparation p){return List.of(SubjectBinding.optional("Work",Set.of((String)((Map<?,?>)intent.get("slots")).get("workId"))));}
 };
 CommandDefinitionResolver resolver(){
  var db=mock(PersistenceService.class);var result=mock(Result.class);var repository=mock(DefinitionRepository.class);
  when(db.run(any(CqnSelect.class))).thenReturn(result);when(result.listOf(Map.class)).thenReturn(List.of(Map.of("ID",definitionId)));
  when(repository.get(org,definitionId)).thenReturn(new Definition(org,definitionId,"published",null,"PUBLISHED","hash","core-v1","1.0.0",List.of(new Definition.NounType("Work",true),new Definition.NounType("TradeItem",true)),List.of(),List.of(new Definition.Verb("closeWork","COMMAND","closeWork","ACTIVE",Map.of())),List.of(),List.of(),List.of(new Definition.Capability("closeWork","1.0.0","core-v1","1.0.0","1.0.0",List.of()))));
  return new CommandDefinitionResolver(db,repository);
 }
 Map<String,Object> intent(List<?> refs){return Map.of("definitionVersion","published","capabilityId","closeWork","intentKind","COMMAND","slots",Map.of("workId",work),"subjectRefs",refs);}
 @Test void unrelatedWorkIsRejectedEvenWhenPresentInAuthorizationScope(){assertThrows(DomainError.class,()->resolver().verifySubjects(context,intent(List.of(Map.of("type","Work","id",other))),handler,prep));}
 @Test void wrongPublishedNounAndUnknownNounCannotAliasWork(){for(String noun:List.of("TradeItem","WORK","Unknown"))assertThrows(DomainError.class,()->resolver().verifySubjects(context,intent(List.of(Map.of("type",noun,"id",work))),handler,prep));}
 @Test void actualTargetAndExplicitlyOptionalEmptySubjectsAreAccepted(){assertEquals(1,resolver().verifySubjects(context,intent(List.of(Map.of("type","Work","id",work))),handler,prep).size());assertEquals(1,resolver().verifySubjects(context,intent(List.of()),handler,prep).size());}
 @Test void duplicateDeclaredSubjectIsRejected(){var ref=Map.of("type","Work","id",work);assertThrows(DomainError.class,()->resolver().verifySubjects(context,intent(List.of(ref,ref)),handler,prep));}
 @Test void requiredSubjectCannotBeOmitted(){CommandHandler required=new CommandHandler(){public Set<String> capabilities(){return handler.capabilities();}public Map<String,Object> execute(DomainContext c,Map<String,Object> i){throw new AssertionError();}public List<SubjectBinding> subjectBindings(DomainContext c,Map<String,Object> i,CommandPreparation p){return List.of(SubjectBinding.required("Work",Set.of(work)));}};assertThrows(DomainError.class,()->resolver().verifySubjects(context,intent(List.of()),required,prep));}
}

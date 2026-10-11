package com.mulino.adapters;

import com.mulino.application.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.services.*;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.*;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
@ServiceName("OntologyService")
public class OntologyCap implements EventHandler {
  private final ApplicationQueries queries;
  private final ApplicationCommands commands;
  private final ObjectMapper json=new ObjectMapper();
  public OntologyCap(ApplicationQueries queries,ApplicationCommands commands){this.queries=queries;this.commands=commands;}
  @On(event={"command","validateCommand"})
  public void command(EventContext context){
    // Plan section 3.4: every command failure the request causes returns the structured command envelope, as REST does.
    Map<String,Object> input;
    try{input=json.readValue((String)context.get("requestJson"),Map.class);}
    catch(Exception malformed){complete(context,DomainError.invalid("Command requestJson must be a JSON object").response());return;}
    if(input==null){complete(context,DomainError.invalid("Command requestJson must be a JSON object").response());return;}
    Map<String,Object> result;var changeSet=context.getChangeSetContext();
    try{result="validateCommand".equals(context.getEvent())?commands.validate(input):commands.executeInChangeSet(input,audit->changeSet.register(new com.sap.cds.services.changeset.ChangeSetListener(){
      @Override public void afterClose(boolean completed){audit.run();}
    }));}
    catch(DomainError failure){result=failure.response();}
    // A command that did not apply leaves no effect: its changeset (a single action, or every operation of an atomic
    // $batch changeset) rolls back instead of committing a transaction the command marked rollback-only.
    if(CommandOutcomes.NOT_APPLIED.contains(result.get("outcome")))changeSet.markForCancel();
    complete(context,result);
  }
  private void complete(EventContext context,Map<String,Object> result){
    try{context.put("result",json.writeValueAsString(result));context.setCompleted();}catch(Exception serialization){throw new IllegalStateException(serialization);}
  }
  @On(event="query")
  public void query(EventContext context){
    try{
      Map<String,Object> input=json.readValue((String)context.get("requestJson"),Map.class);
      var response=queries.query(QueryRequests.parse((String)context.get("operation"),input));
      context.put("result",json.writeValueAsString(response));context.setCompleted();
    }catch(DomainError failure){
      // Domain envelope stays intact rather than becoming an adapter-specific outcome.
      try{context.put("result",json.writeValueAsString(failure.response()));context.setCompleted();}catch(Exception serialization){throw new IllegalStateException(serialization);}
    }catch(Exception failure){throw new ServiceException(ErrorStatuses.BAD_REQUEST,"Invalid ontology query");}
  }
}

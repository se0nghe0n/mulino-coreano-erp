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
    try{
      Map<String,Object> input=json.readValue((String)context.get("requestJson"),Map.class);
      var result="validateCommand".equals(context.getEvent())?commands.validate(input):commands.execute(input);
      context.put("result",json.writeValueAsString(result));context.setCompleted();
    }catch(DomainError failure){try{context.put("result",json.writeValueAsString(failure.response()));context.setCompleted();}catch(Exception serialization){throw new IllegalStateException(serialization);}}
    catch(Exception failure){throw new ServiceException(ErrorStatuses.BAD_REQUEST,"Invalid ontology command");}
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

package org.mulino.verification.modelbinding;
import org.mulino.verification.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.*;

/** Explicit standalone preparation/RED entry point; no model/client adapter is installed. */
public final class ModelBindingMain {
    public static void main(String[] args) throws Exception {System.exit(execute(Path.of(System.getProperty("repo.root",".")),args.length==0?"prepare":args[0],args.length>1?args[1]:"SIT"));}
    public static int execute(Path root,String mode) throws Exception {return execute(root,mode,"SIT");}
    public static int execute(Path root,String mode,String profile) throws Exception {
        BindingContract c=new BindingContract(root);ObjectNode report=c.prepare();report.put("command",System.getProperty("verification.command","ModelBindingMain "+mode));report.put("exitCode",mode.equals("prepare")?0:2);report.set("versions",Json.parse("{\"binding\":\"1.0.0\",\"mapping\":\"model-binding-v1\"}"));
        String name="preparation";
        if(mode.equals("red")) {name=profile.equals("SIT")?"red":"red-uat";report.put("profile",profile);ArrayNode cases=Json.array();for(var e:c.registry.path("cases")){ModelBindingRunner r=new ModelBindingRunner(c,e.path("caseId").asText(),profile,new UnimplementedDriver(),profile.equals("SIT")?new AgentRunner.Scripted():new AgentRunner.Actual((id,route,actor,text,context)->StepResult.missing(id,"NOT_IMPLEMENTED: actual client absent")));r.execute("install");for(var t:c.binding(e.path("caseId").asText()).path("turns"))for(var step:t.path("steps"))r.execute(t.path("id").asText()+"/"+step.asText());r.verifyComplete();cases.add(r.evidence());}report.set("perCase",cases);report.put("status","NOT_RUN").put("reason","NOT_IMPLEMENTED: actual product API/DB/MCP/model adapters absent");}
        else BindingContract.require(mode.equals("prepare"),"prepare|red required");
        Json.write(c.validator.path("verification/harness/target/evidence/model-binding-"+name+".json"),report);System.out.println(report.toPrettyString());return mode.equals("prepare")?0:2;
    }
}

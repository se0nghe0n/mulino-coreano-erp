package org.mulino.verification.nativeclient;

import org.mulino.verification.*;
import org.mulino.verification.actual.ActualConfiguration;
import java.nio.file.*;
import java.util.*;

/** Focused installer inventory/PG readback entrypoint; never executes a scenario, native client, or model. */
public final class ModelFixtureProbe {
    public static void main(String[] args) throws Exception {
        if(args.length==3&&args[0].equals("inventory-all")&&args[1].equals("ALL")) {
            Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize();var all=Json.array();
            for(int n=1;n<=60;n++){String id="M%02d".formatted(n);Path fixture=root.resolve("verification/model-binding/cases/"+id+"/fixture.json");var inventory=ModelFixtureInstaller.inventory(Json.read(fixture),Json.read(root.resolve("verification/model-installation/v1/inputs/"+id+"-isolated.json")),Json.sha256(fixture));inventory.put("caseId",id);all.add(inventory);}
            Path out=root.resolve(args[2]).normalize();if(!out.startsWith(root))throw new IllegalArgumentException("Output escape");Files.createDirectories(out.getParent());Json.MAPPER.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),all);System.out.println(all.size()+" inventories; no product execution");return;
        }
        if(args.length!=3||!Set.of("inventory","install","install-original-authority").contains(args[0])||!args[1].matches("M[0-9]{2}"))throw new IllegalArgumentException("inventory|install Mxx output.json");
        Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize();Path fixture=root.resolve("verification/model-binding/cases/"+args[1]+"/fixture.json");String hash=Json.sha256(fixture);
        Path input=root.resolve("verification/model-installation/v1/inputs/"+args[1]+"-isolated.json");var f=Json.read(fixture);var inputs=Json.read(input);if(args[0].equals("install-original-authority"))((com.fasterxml.jackson.databind.node.ObjectNode)inputs).remove("authorityCorrection");
        var bundle=Json.object().put("fixtureHash",hash);bundle.set("fixture",f);
        var result=args[0].equals("inventory")?ModelFixtureInstaller.inventory(f,inputs,hash):new ModelFixtureInstaller(root,ActualConfiguration.environment(System.getenv()),System.getProperty("verification.actual.blobRoot")==null?null:Path.of(System.getProperty("verification.actual.blobRoot"))).install(bundle,inputs);
        Path output=root.resolve(args[2]).normalize();if(!output.startsWith(root))throw new IllegalArgumentException("Probe output escapes root");Files.createDirectories(output.getParent());Json.MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.toFile(),result);
        System.out.println(result.path("installable").asBoolean()?"INSTALLABLE":"EXPLICIT_INPUTS_REQUIRED");
    }
    private ModelFixtureProbe() {}
}

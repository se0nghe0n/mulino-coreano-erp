package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.*;
import com.mulino.application.core.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Executes real handler metadata methods without a Spring context, DB or business command. */
class PublicCapabilityInventoryTest {
    static final Path ROOT=Path.of(System.getProperty("repo.root","..")).toAbsolutePath().normalize();
    static final ObjectMapper JSON=new ObjectMapper();
    record Registration(String capability,String source,String profile,CommandHandler handler) {}
    static List<Registration> registered;
    static Map<String,JsonNode> inventory;

    @BeforeAll static void discover() throws Exception {
        var entries=new TreeMap<String,JsonNode>();
        for(var entry:JSON.readTree(ROOT.resolve("contracts/acceptance-capabilities.json").toFile()).path("capabilities"))
            assertNull(entries.put(entry.path("id").asText(),entry),"Duplicate inventory entry");
        inventory=entries;registered=new ArrayList<>();
        Path source=ROOT.resolve("backend/src/main/java");
        try(var paths=Files.walk(source)) {
            for(Path path:paths.filter(p->p.toString().endsWith(".java")).sorted().toList()) {
                // Load without initialization; only actual CommandHandler types are instantiated as metadata mocks.
                String name=source.relativize(path).toString().replace(java.io.File.separatorChar,'.').replaceFirst("\\.java$","");
                Class<?> type=Class.forName(name,false,PublicCapabilityInventoryTest.class.getClassLoader());
                if(type.isInterface()||!CommandHandler.class.isAssignableFrom(type))continue;
                assertTrue(type.isAnnotationPresent(Component.class)||type.isAnnotationPresent(Service.class),name+" is not a scanned bean; classify explicitly");
                var profile=type.getAnnotation(Profile.class);
                String scope=profile==null?null:String.join(",",profile.value());
                var handler=(CommandHandler)mock(type,CALLS_REAL_METHODS);
                assertTrue(handler.publishesSlots(),name+" must be explicitly classified if internal-only");
                for(String cap:handler.capabilities())registered.add(new Registration(cap,ROOT.relativize(path).toString(),scope,handler));
            }
        }
        assertFalse(registered.isEmpty());
        String output=System.getProperty("capability.inventory.evidence");
        if(output!=null){
            var path=Path.of(output);Files.createDirectories(path);
            var rows=registered.stream().map(r->{var row=new LinkedHashMap<String,Object>();row.put("capability",r.capability());row.put("source",r.source());row.put("profile",r.profile());row.put("semanticVersion",r.handler().semanticVersion());row.put("intentKinds",new TreeSet<>(r.handler().intentKinds()));return row;}).toList();
            JSON.writerWithDefaultPrettyPrinter().writeValue(path.resolve("registered-handlers.json").toFile(),rows);
        }
    }
    static Set<String> normal(){return registered.stream().filter(r->r.profile()==null).map(Registration::capability).collect(Collectors.toCollection(TreeSet::new));}
    static Map<String,Registration> byName(){var out=new TreeMap<String,Registration>();for(var r:registered)assertNull(out.put(r.capability(),r),"Duplicate registered handler "+r.capability());return out;}

    @Test void everyNormalRegisteredCommandHasPublicInventoryAndSlotContract() {
        var missing=new TreeSet<>(normal());missing.removeAll(inventory.keySet());
        assertTrue(missing.isEmpty(),"Registered product handlers absent from public inventory: "+missing);
        assertEquals(normal(),new TreeSet<>(CapabilitySlots.unconditional()));
        for(var r:registered)if(r.profile()==null){
            assertTrue(r.handler().intentKinds().contains(inventory.get(r.capability()).path("kind").asText()),r.capability());
            CapabilitySlots.allowed(r.capability());
        }
    }

    @Test void implementedPublicCommandsHaveRealHandlerMetadataAndSupportedForms() throws Exception {
        var actual=byName();var slots=JSON.readTree(ROOT.resolve("contracts/capability-slots.json").toFile()).path("capabilities");
        for(var entry:inventory.values())if(entry.path("implemented").asBoolean()&&!entry.path("kind").asText().equals("QUERY")){
            var r=actual.get(entry.path("id").asText());assertNotNull(r,entry.toString());assertNull(r.profile(),entry.toString());
            assertEquals(r.handler().semanticVersion(),entry.path("semanticVersion").asText());
        }
        assertEquals(actual.keySet(),CapabilitySlots.capabilities());
        for(var r:registered){
            assertEquals(r.profile(),slots.path(r.capability()).path("profile").isMissingNode()?null:slots.path(r.capability()).path("profile").asText());
            for(var fields:slots.path(r.capability()).path("forms"))for(var field:fields)assertTrue(CapabilitySlots.allowed(r.capability()).contains(field.asText()));
        }
        assertFalse(normal().contains("fixtureExternalEffect"));assertFalse(inventory.containsKey("fixtureExternalEffect"));
        var unsupported=new TreeSet<String>();
        for(var entry:inventory.values())if(!entry.path("kind").asText().equals("QUERY")&&!normal().contains(entry.path("id").asText())){
            assertFalse(entry.path("implemented").asBoolean());unsupported.add(entry.path("id").asText());
        }
        assertEquals(Set.of("retrySafeCommand","recordRelation","emergencyRepair"),unsupported,
            "Do not publish a case-only or internal-only command as supported");
    }

    @Test void publicRouterManifestMatchesNormalHandlersAndRejectsOriginalUnknownProbes() {
        var manager=mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(invocation->new SimpleTransactionStatus());
        var commands=new ApplicationCommands(registered.stream().filter(r->r.profile()==null).map(Registration::handler).distinct().toList(),
            mock(ReadAuthorizer.class),mock(CommandRepository.class),mock(ExecutionClock.class),mock(CommandDefinitionResolver.class),
            mock(org.springframework.beans.factory.ObjectProvider.class),mock(org.springframework.beans.factory.ObjectProvider.class),
            mock(org.springframework.beans.factory.ObjectProvider.class),mock(org.springframework.beans.factory.ObjectProvider.class),manager);
        assertEquals(normal(),new TreeSet<>(commands.operations()));
        assertEquals(normal(),commands.manifest().stream().map(r->r.get("capabilityId").toString()).collect(Collectors.toCollection(TreeSet::new)));
        for(String probe:List.of("retrySafeCommand","recordRelation","emergencyRepair","structureIntent","unknownInventoryMutation")){
            assertFalse(commands.operations().contains(probe));
            var request=Map.<String,Object>of("intentKind","COMMAND","definitionVersion","definition-v1","capabilityId",probe,
                "slots",Map.of(),"subjectRefs",List.of(),"provenance",Map.of(),"commandIdempotencyKey","inventory-negative-"+probe);
            var error=assertThrows(DomainError.class,()->commands.validate(request));
            assertEquals("VERSION_UNSUPPORTED",error.code(),probe);
            assertEquals("HELD",error.outcome(),probe);assertEquals(Map.of(),error.response().get("effects"));
        }
    }
}

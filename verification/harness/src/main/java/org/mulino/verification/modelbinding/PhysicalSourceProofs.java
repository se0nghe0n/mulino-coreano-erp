package org.mulino.verification.modelbinding;

import com.fasterxml.jackson.databind.JsonNode;
import org.mulino.verification.Json;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static org.mulino.verification.modelbinding.PhysicalRecipeRows.*;

/** Objective source integrity from physical SQL and retained original bytes; READ visibility is checked separately. */
public final class PhysicalSourceProofs {
    private static final String E="mulino_evidence_";
    public static boolean visible(PhysicalRecipeRows r,JsonNode row){return at(row,"recordedAt",r.knownAt)&&at(row,"effectiveFrom",r.asOf)&&(!row.hasNonNull("effectiveUntil")||!at(row,"effectiveUntil",r.asOf));}
    public static byte[] bytes(PhysicalRecipeRows r,JsonNode document){
        require(document.path("availability").asText().equals("AVAILABLE")&&document.hasNonNull("blobId"),"SOURCE_DOCUMENT_NOT_AVAILABLE");
        JsonNode proof=null;for(JsonNode candidate:r.capture.path("sourceDocuments"))if(candidate.path("documentId").equals(document.path("ID"))){require(proof==null,"SOURCE_DOCUMENT_PROOF_DUPLICATE");proof=candidate;}
        require(proof!=null&&proof.path("status").asText().equals("CAPTURED"),"SOURCE_DOCUMENT_BYTES_UNOBSERVED");
        byte[] bytes;try{bytes=Base64.getDecoder().decode(Json.required(proof,"bytesBase64"));require(bytes.length<=4*1024*1024,"SOURCE_DOCUMENT_BYTES_TOO_LARGE");String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));require(hash.equals(document.path("sha256").asText())&&hash.equals(proof.path("sha256").asText())&&bytes.length==proof.path("sizeBytes").asLong(-1)&&bytes.length==document.path("byteLength").asLong(-1),"SOURCE_DOCUMENT_BYTES_CHANGED");}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}return bytes;
    }
    public static boolean verified(PhysicalRecipeRows r,String canonical,String actor){
        JsonNode occurrence=r.one(E+"CanonicalOccurrences",canonical);if(!visible(r,occurrence)||!occurrence.path("valueState").asText().equals("KNOWN")||!occurrence.path("reassessmentState").asText().equals("COMPLETE"))return false;
        var claims=r.rows(E+"Claims");var events=r.rows(E+"Events");var docs=r.rows(E+"DocumentVersions");var inbox=r.rows(E+"InboxRecords");var profiles=r.rows(E+"SourceProfiles");
        for(JsonNode verification:r.rows(E+"Verifications")) {
            if(!verification.path("canonicalOccurrenceId").asText().equals(canonical)||!verification.path("verdict").asText().equals("VERIFIED")||!visible(r,verification)||!List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->verification.path(k).asBoolean(false)))continue;
            JsonNode claim=single(claims.stream().filter(c->c.path("ID").equals(verification.path("claimId"))).toList()),event=single(events.stream().filter(e->e.path("ID").equals(claim.path("eventId"))).toList()),doc=single(docs.stream().filter(d->d.path("ID").equals(verification.path("basisDocumentId"))).toList());
            if(!visible(r,claim)||!visible(r,event)||!visible(r,doc)||!claim.path("valueState").asText().equals("KNOWN")||!event.path("valueState").asText().equals("KNOWN"))continue;
            bytes(r,doc);
            if(actor!=null&&(!allowed(r,actor,claim)||!allowed(r,actor,event)||!allowed(r,actor,doc)))continue;
            if(events.stream().anyMatch(e->visible(r,e)&&(e.path("invalidatesId").equals(event.path("ID"))||e.path("supersedesId").equals(event.path("ID"))))||docs.stream().anyMatch(d->visible(r,d)&&d.path("supersedesId").equals(doc.path("ID")))||claims.stream().anyMatch(c->visible(r,c)&&c.path("supersedesId").equals(claim.path("ID"))))continue;
            var variants=inbox.stream().filter(i->visible(r,i)&&i.path("sourceNamespace").equals(event.path("sourceNamespace"))&&i.path("externalEventId").equals(event.path("externalEventId"))&&i.path("sourceVersion").equals(event.path("sourceVersion"))).toList();if(variants.size()!=1||!variants.getFirst().path("eventId").equals(event.path("ID"))||!variants.getFirst().path("payloadHash").equals(event.path("payloadHash"))||variants.getFirst().path("state").asText().equals("CONFLICT"))continue;
            if(!claim.path("documentVersionId").equals(doc.path("ID")))continue;
            boolean matching=true;for(String key:List.of("subjectKind","subjectId","sourceProfileId"))matching&=claim.path(key).equals(event.path(key))&&claim.path(key).equals(doc.path(key))&&claim.path(key).equals(occurrence.path(key));
            for(String key:List.of("quantity","unit","effectiveFrom","effectiveUntil","timePrecision","timeZone"))matching&=claim.path(key).equals(occurrence.path(key));
            for(String key:List.of("effectiveFrom","effectiveUntil","timePrecision","timeZone"))matching&=claim.path(key).equals(event.path(key));
            matching&=event.path("kind").equals(occurrence.path("kind"))&&event.path("payloadHash").equals(doc.path("sha256"));
            JsonNode profile=single(profiles.stream().filter(p->p.path("ID").equals(event.path("sourceProfileId"))).toList());matching&=visible(r,profile)&&profile.path("namespace").equals(event.path("sourceNamespace"))&&profile.path("policyVersion").equals(verification.path("policyVersion"));
            matching&=profiles.stream().filter(p->visible(r,p)&&p.path("namespace").equals(profile.path("namespace"))).count()==1;
            matching&=Json.parse(new String(bytes(r,doc),StandardCharsets.UTF_8)).equals(Json.parse(r.text(event,"payload")));
            if(matching)return true;
        }return false;
    }
    public static Map<String,Set<String>> targets(JsonNode row){var targets=new LinkedHashMap<String,Set<String>>();var ids=new HashSet<String>();ids.add(Json.required(row,"ID"));if(row.hasNonNull("subjectId"))ids.add(row.path("subjectId").asText());targets.put("TARGET",ids);for(var pair:Map.of("itemId","ITEM","placeId","PLACE","workId","WORK","sourceProfileId","SOURCE").entrySet())if(row.hasNonNull(pair.getKey()))targets.put(pair.getValue(),Set.of(row.path(pair.getKey()).asText()));return targets;}
    private static boolean allowed(PhysicalRecipeRows r,String actor,JsonNode row){return PhysicalAuthorization.allowed(r,actor,"getEvidence",targets(row),new HashSet<>());}
    private PhysicalSourceProofs() {}
}

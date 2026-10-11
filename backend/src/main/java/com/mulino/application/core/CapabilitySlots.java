package com.mulino.application.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;

/**
 * User decision 2026-10-11 (3): the published per-capability slot contract (contracts/capability-slots.json, packaged as
 * the classpath resource contracts/capability-slots.json) is the product's only slot-name registry. The command pipeline
 * rejects a slot name the capability does not list before its handler runs, and handlers that distinguish request forms
 * read the form from here, so the published list and the accepted names cannot drift.
 */
public final class CapabilitySlots {
  public static final String RESOURCE="contracts/capability-slots.json";
  private static final Map<String,Set<String>> SLOTS;
  private static final Map<String,Map<String,Set<String>>> FORMS;
  /** Capabilities whose handler exists only in a named Spring profile (e.g. verification fixtures). */
  private static final Map<String,String> PROFILE;
  static {
    try(var in=CapabilitySlots.class.getClassLoader().getResourceAsStream(RESOURCE)){
      if(in==null)throw new IllegalStateException("Capability slot contract missing: "+RESOURCE);
      JsonNode root=new ObjectMapper().readTree(in);var slots=new TreeMap<String,Set<String>>();var forms=new TreeMap<String,Map<String,Set<String>>>();var profiles=new TreeMap<String,String>();
      for(var it=root.path("capabilities").fields();it.hasNext();){
        var e=it.next();var names=names(e.getValue().path("slots"));slots.put(e.getKey(),names);
        var f=new TreeMap<String,Set<String>>();for(var fi=e.getValue().path("forms").fields();fi.hasNext();){var form=fi.next();var subset=names(form.getValue());if(!names.containsAll(subset))throw new IllegalStateException("Form "+form.getKey()+" of "+e.getKey()+" lists unpublished slots");f.put(form.getKey(),subset);}
        forms.put(e.getKey(),Collections.unmodifiableMap(f));if(e.getValue().hasNonNull("profile"))profiles.put(e.getKey(),e.getValue().path("profile").asText());
      }
      SLOTS=Collections.unmodifiableMap(slots);FORMS=Collections.unmodifiableMap(forms);PROFILE=Collections.unmodifiableMap(profiles);
    }catch(IOException failure){throw new IllegalStateException("Unreadable capability slot contract",failure);}
  }
  private CapabilitySlots(){}
  private static Set<String> names(JsonNode array){var out=new TreeSet<String>();for(JsonNode n:array){if(!n.isTextual()||n.asText().isBlank()||!out.add(n.asText()))throw new IllegalStateException("Invalid capability slot name "+n);}return Collections.unmodifiableSet(out);}
  public static Set<String> capabilities(){return SLOTS.keySet();}
  /** Published capabilities a handler must provide in every profile (those not bound to a named profile). */
  public static Set<String> unconditional(){var out=new TreeSet<String>(SLOTS.keySet());out.removeAll(PROFILE.keySet());return Collections.unmodifiableSet(out);}
  /** Every slot name the capability accepts (union of its forms). */
  public static Set<String> allowed(String capability){var s=SLOTS.get(capability);if(s==null)throw DomainError.unsupported();return s;}
  /** One published request form of a capability. */
  public static Set<String> form(String capability,String form){var f=FORMS.getOrDefault(capability,Map.of()).get(form);if(f==null)throw new IllegalStateException("Unpublished slot form "+capability+"/"+form);return f;}
  /** Pipeline check: a slot name outside the capability's published list is a type error (plan §3.3 slot before policy). */
  public static void require(String capability,Map<?,?> slots){
    var allowed=allowed(capability);
    for(Object key:slots.keySet())if(!(key instanceof String name)||!allowed.contains(name))throw DomainError.invalid("Unsupported slot for "+capability+": "+(String.valueOf(key).length()>80?String.valueOf(key).substring(0,80):key));
  }
}

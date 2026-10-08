package com.mulino.application.responsibility;

import com.mulino.application.core.DomainError;
import java.util.*;

/** HELD with the retained human responsibility; effects are always empty. */
public class ResponsibilityHeld extends DomainError {
 private final Map<String,Object> responsibility;
 public ResponsibilityHeld(String code,String message,Map<String,Object> responsibility){super("HELD",code,message);this.responsibility=Map.copyOf(responsibility);}
 public Map<String,Object> responsibility(){return responsibility;}
 @Override public Map<String,Object> response(){
  var result=new LinkedHashMap<String,Object>(super.response());result.put("responsibility",responsibility);return Map.copyOf(result);
 }
}

package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.domain.runtime.RuntimeRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Converts a public lease reference to server-owned execution metadata. */
@Service
public class WorkerClaimResolver {
  private final ReadAuthorizer auth;
  private final ExecutionClock clock;
  private final RuntimeRepository repository;

  public WorkerClaimResolver(ReadAuthorizer auth,ExecutionClock clock,RuntimeRepository repository){
    this.auth=auth;this.clock=clock;this.repository=repository;
  }

  @Transactional
  public Map<String,Object> resolve(Map<?,?> reference){
    if(reference==null||!reference.keySet().equals(Set.of("claimId","fencingToken"))
        ||!(reference.get("claimId") instanceof String id)
        ||!(reference.get("fencingToken") instanceof Number number))throw DomainError.forbidden();
    long token;
    try{
      if(!UUID.fromString(id).toString().equals(id))throw new IllegalArgumentException();
      token=new java.math.BigDecimal(number.toString()).longValueExact();
      if(token<1)throw new IllegalArgumentException();
    }catch(IllegalArgumentException|ArithmeticException invalid){throw DomainError.forbidden();}
    return repository.externalClaim(auth.context(clock.instant(),clock.instant()),id,token);
  }
}

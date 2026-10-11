package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mulino.application.core.*;
import com.mulino.application.definitions.DefinitionQueries;
import com.mulino.domain.definitions.DefinitionRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

/** getDefinition of an ID the organization does not hold answers FORBIDDEN (plan §3.4), not HTTP 500. */
class DefinitionQueriesUnavailableTest {
  @Test void unknownDefinitionIsAnUnavailableScope() {
    var repository=mock(DefinitionRepository.class);var authorizer=mock(ReadAuthorizer.class);
    when(repository.get(anyString(),anyString())).thenThrow(new NoSuchElementException("Definition unavailable"));
    var queries=new DefinitionQueries(repository,authorizer);
    var c=new DomainContext("org","actor","owner",Instant.parse("2026-10-07T09:00:00Z"),Instant.parse("2026-10-07T09:00:00Z"));
    var q=new QueryRequest("getDefinition",UUID.randomUUID().toString(),Map.of(),Map.of(),10,null,null,null,null,null);
    assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->queries.query(c,q)).code());
  }
}

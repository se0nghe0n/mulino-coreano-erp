package com.mulino.application.core;

import java.util.Set;

public interface QueryHandler {
  Set<String> operations();
  QueryResult query(DomainContext context, QueryRequest request);
}

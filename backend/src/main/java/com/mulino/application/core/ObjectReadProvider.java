package com.mulino.application.core;

import java.util.Set;

/** A domain owns noun reads without registering a second getObject handler. */
public interface ObjectReadProvider {
  Set<String> objectTypes();
  /** Supports getObject/searchObjects using the same data and authorization as domain verb reads. */
  QueryResult query(DomainContext context, QueryRequest request);
}

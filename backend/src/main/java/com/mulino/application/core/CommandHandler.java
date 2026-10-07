package com.mulino.application.core;

import java.util.Map;
import java.util.Set;

/** Explicit action ports only; S1 exposes no product write dispatcher. */
public interface CommandHandler {
  Set<String> capabilities();
  Map<String,Object> execute(DomainContext context, Map<String,Object> intent);
}

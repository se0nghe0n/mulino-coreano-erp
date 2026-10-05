package com.mulinocoreano.backend.recall;

import java.io.IOException;
import java.nio.file.*;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class RecallDemoFixture {
  private RecallDemoFixture() {}

  public static void load(JdbcClient jdbc) throws IOException {
    Path p = Path.of("../database/seed/recall_demo.sql");
    if (!Files.exists(p)) p = Path.of("database/seed/recall_demo.sql");
    jdbc.sql(Files.readString(p)).update();
  }
}

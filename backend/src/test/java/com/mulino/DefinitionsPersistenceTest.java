package com.mulino;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.sql.*;
import java.nio.file.*;

class DefinitionsPersistenceTest {
 @Test void actualPostgresPublishedVersionsAreImmutableAndOrganizationBound() throws Exception {
  try(var pg=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280")) {
   pg.start();
   try(var c=DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());var s=c.createStatement()) {
    // Minimal identity dependency is sufficient for the isolated V4 constraint test.
    s.execute("CREATE TABLE mulino_identity_Organizations(ID VARCHAR(36) PRIMARY KEY)");
    s.execute(Files.readString(Path.of("../database/migrations/V4__definitions.sql")));
    s.execute("INSERT INTO mulino_identity_Organizations VALUES('org-a'),('org-b')");
    s.execute("INSERT INTO mulino_definitions_DefinitionVersions(organizationId,ID,version,state,contentHash,content,evaluatorVersion,schemaVersion) VALUES('org-a','v1','definition-v1','DRAFT','"+"a".repeat(64)+"','{}','core-v1','1.0.0')");
    s.execute("INSERT INTO mulino_definitions_NounTypes(organizationId,ID,definitionVersionId,name,core) VALUES('org-a','n1','v1','Place',true)");
    assertThrows(SQLException.class,()->s.execute("INSERT INTO mulino_definitions_NounTypes(organizationId,ID,definitionVersionId,name) VALUES('org-b','n2','v1','Place')"));
    s.execute("UPDATE mulino_definitions_DefinitionVersions SET state='PUBLISHED' WHERE ID='v1'");
    assertThrows(SQLException.class,()->s.execute("UPDATE mulino_definitions_DefinitionVersions SET content='changed' WHERE ID='v1'"));
    assertThrows(SQLException.class,()->s.execute("DELETE FROM mulino_definitions_DefinitionVersions WHERE ID='v1'"));
    assertThrows(SQLException.class,()->s.execute("UPDATE mulino_definitions_NounTypes SET name='Changed' WHERE ID='n1'"));
    assertThrows(SQLException.class,()->s.execute("INSERT INTO mulino_definitions_NounTypes(organizationId,ID,definitionVersionId,name) VALUES('org-a','n2','v1','Customer')"));
   }
  }
 }
}

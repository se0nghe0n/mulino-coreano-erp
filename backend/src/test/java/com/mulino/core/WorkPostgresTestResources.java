package com.mulino.core;

import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** One test class owns one host. Reusing seed methods must never initialize or close another class's host. */
final class WorkPostgresTestResources implements AutoCloseable {
 final PostgreSQLContainer pg=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
 final KeyPair key;
 final Path publicKeyPath,blobRoot;
 private final String owner;
 private final String containerId;
 private final BusinessDiscoveryPostgresTarget externalDatabase;
 WorkPostgresTestResources(String owner){
  this.owner=owner;this.externalDatabase=null;Path publicPath=null,blobPath=null;
  try{
   pg.start();var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);key=generator.generateKeyPair();
   blobPath=Files.createTempDirectory("mulino-"+owner+"-blob-");publicPath=Files.createTempFile("mulino-"+owner+"-public-",".pem");
   Files.writeString(publicPath,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(key.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");
   publicKeyPath=publicPath;blobRoot=blobPath;containerId=pg.getContainerId();
   System.out.println("WORK_TEST_RESOURCE_OPEN "+new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("owner",owner,"containerId",containerId,"publicKeyPath",publicKeyPath.toString(),"blobRoot",blobRoot.toString(),"jvmPid",ProcessHandle.current().pid())));
  }catch(Exception failure){
   try{pg.stop();if(publicPath!=null)Files.deleteIfExists(publicPath);if(blobPath!=null)deleteTree(blobPath);}catch(Exception cleanup){failure.addSuppressed(cleanup);}
   throw new IllegalStateException("Cannot start owned test resources: "+owner,failure);
  }
 }
 static WorkPostgresTestResources forBusinessDiscovery(String owner){
  var target=BusinessDiscoveryPostgresTarget.fromEnvironment(System.getenv());
  return target.map(value->new WorkPostgresTestResources(owner,value))
   .orElseGet(()->new WorkPostgresTestResources(owner));
 }
 private WorkPostgresTestResources(String owner,BusinessDiscoveryPostgresTarget target){
  this.owner=owner;this.externalDatabase=target;this.containerId=null;Path publicPath=null,blobPath=null;
  try{
   target.verifyFreshDatabase();
   var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);key=generator.generateKeyPair();
   blobPath=Files.createTempDirectory("mulino-"+owner+"-blob-");publicPath=Files.createTempFile("mulino-"+owner+"-public-",".pem");
   Files.writeString(publicPath,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(key.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");
   publicKeyPath=publicPath;blobRoot=blobPath;
   System.out.println("WORK_TEST_RESOURCE_OPEN "+new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("owner",owner,"databaseMode","coordinator-owned-disposable","publicKeyPath",publicKeyPath.toString(),"blobRoot",blobRoot.toString(),"jvmPid",ProcessHandle.current().pid())));
  }catch(Exception failure){
   try{if(publicPath!=null)Files.deleteIfExists(publicPath);if(blobPath!=null)deleteTree(blobPath);}catch(Exception cleanup){failure.addSuppressed(cleanup);}
   throw new IllegalStateException("Cannot initialize disposable business discovery test resources",failure);
  }
 }
 void properties(DynamicPropertyRegistry r){
  if(externalDatabase==null){
   r.add("spring.datasource.url",pg::getJdbcUrl);r.add("spring.datasource.username",pg::getUsername);r.add("spring.datasource.password",pg::getPassword);
  }else{
   r.add("spring.datasource.url",externalDatabase::jdbcUrl);r.add("spring.datasource.username",externalDatabase::username);r.add("spring.datasource.password",externalDatabase::password);
  }
  r.add("JWT_PUBLIC_KEY",publicKeyPath::toString);r.add("JWT_ISSUER",()->"https://mulino.local.invalid");r.add("JWT_AUDIENCE",()->"mulino-platform");r.add("mulino.evidence.blob-root",blobRoot::toString);
 }
 @Override public void close()throws Exception{
  if(externalDatabase==null){
   try{pg.stop();}finally{try{Files.deleteIfExists(publicKeyPath);}finally{deleteTree(blobRoot);}}
   System.out.println("WORK_TEST_RESOURCE_CLOSED "+new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("owner",owner,"containerId",containerId,"publicKeyAbsent",!Files.exists(publicKeyPath),"blobAbsent",!Files.exists(blobRoot),"jvmPid",ProcessHandle.current().pid())));
  }else{
   try{Files.deleteIfExists(publicKeyPath);}finally{deleteTree(blobRoot);}
   System.out.println("WORK_TEST_RESOURCE_CLOSED "+new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("owner",owner,"databaseMode","coordinator-owned-disposable","publicKeyAbsent",!Files.exists(publicKeyPath),"blobAbsent",!Files.exists(blobRoot),"jvmPid",ProcessHandle.current().pid())));
  }
 }
 private static void deleteTree(Path root)throws java.io.IOException{if(Files.exists(root))try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
}

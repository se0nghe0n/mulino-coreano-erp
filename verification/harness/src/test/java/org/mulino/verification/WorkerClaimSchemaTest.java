package org.mulino.verification;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkerClaimSchemaTest {
  final Path root=Path.of(System.getProperty("repo.root"));
  @Test void declaredClaimControlRequiresBoundedTtlAndIdentity() throws Exception {
    var validator=new ContractValidator(root);
    var c=Json.read(root.resolve("verification/cases/C3/case.json"));
    var sub=(ObjectNode)c.path("subcases").get(0);
    var action=(ObjectNode)sub.path("actions").get(0);
    action.removeAll();action.put("id","lease").put("kind","control");action.putArray("evidenceRefs").add("lease:actual-artifact");
    action.set("control",Json.parse("{\"type\":\"workerClaim\",\"operation\":\"issue\",\"parameters\":{\"actorRef\":\"reader\",\"workId\":{\"$alias\":\"WORK\"},\"capabilityId\":\"activateWork\",\"commandId\":\"request\",\"workerId\":\"case-worker\",\"ttlSeconds\":5}}"));
    assertDoesNotThrow(()->validator.schema("contracts/acceptance-case.schema.json",c));
    var params=(ObjectNode)action.at("/control/parameters");params.put("ttlSeconds",301);
    assertThrows(IllegalArgumentException.class,()->validator.schema("contracts/acceptance-case.schema.json",c));
    params.put("ttlSeconds",5);params.remove("actorRef");
    assertThrows(IllegalArgumentException.class,()->validator.schema("contracts/acceptance-case.schema.json",c));
  }
}

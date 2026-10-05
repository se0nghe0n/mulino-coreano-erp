package com.mulinocoreano.backend.governance;

import com.mulinocoreano.backend.security.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Typed adapter boundary: reads do not enter a gate; each write names its required role. */
@Component
public class GovernancePolicy {
    public enum Action {
        PURCHASE_CREATE("MANAGER"), INBOUND_STATUS("QC"), RECALL_CREATE("ADMIN"), PRODUCTION_RECALL("ADMIN");
        final String role;
        Action(String role) { this.role=role; }
    }
    private final JdbcClient jdbc;
    public GovernancePolicy(JdbcClient jdbc) { this.jdbc=jdbc; }
    public HumanActor requireHuman(ErpActor actor, Action action) {
        if (!(actor instanceof HumanActor human) || !action.role.equals(human.role()))
            throw new AccessDeniedException("Human role required");
        if (!jdbc.sql("SELECT is_active AND role::text=:role FROM users WHERE user_id=:id FOR UPDATE")
                .param("role",action.role).param("id",human.userId()).query(Boolean.class).optional().orElse(false))
            throw new AccessDeniedException("Active database role required");
        return human;
    }
}

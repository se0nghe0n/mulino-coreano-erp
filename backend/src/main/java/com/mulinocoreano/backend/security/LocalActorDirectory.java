package com.mulinocoreano.backend.security;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import java.util.Locale;

@Component
@Profile("local")
public class LocalActorDirectory {
    private final JdbcClient jdbc;
    private final LocalAuthProperties properties;

    public LocalActorDirectory(JdbcClient jdbc, LocalAuthProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    private record StoredRole(String role, boolean active) {}

    public HumanActor humanForRole(String role) {
        String normalized = LocalActorCapabilities.normalizeRole(role);
        LocalActorCapabilities.forRole(normalized);
        String email = "local-" + normalized.toLowerCase(Locale.ROOT) + "@mulino.local";
        long userId =
                jdbc.sql(
                                """
                                INSERT INTO users(name, email, password, role)
                                VALUES (:name, :email, 'local-stub-only', CAST(:role AS user_role))
                                ON CONFLICT (email) DO UPDATE SET email=EXCLUDED.email
                                RETURNING user_id
                                """)
                        .param("name", "Local " + normalized)
                        .param("email", email)
                        .param("role", normalized)
                        .query(Long.class)
                        .single();
        var stored = jdbc.sql("SELECT role::text, is_active FROM users WHERE user_id=:id")
                .param("id", userId).query((rs, row) -> new StoredRole(rs.getString(1), rs.getBoolean(2))).single();
        if (!stored.active()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        return new HumanActor(
                properties.issuer(),
                "local|" + normalized.toLowerCase(Locale.ROOT),
                userId,
                "Local " + normalized,
                stored.role(),
                LocalActorCapabilities.forRole(stored.role()));
    }
}

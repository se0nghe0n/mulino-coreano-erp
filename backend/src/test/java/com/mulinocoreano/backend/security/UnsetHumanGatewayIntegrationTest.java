package com.mulinocoreano.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 목표 2·5: 미구성 gateway는 역할 헤더를 신뢰하지 않는다. */
@SpringBootTest(properties="mulino.local-auth.human-secret=")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UnsetHumanGatewayIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Test void unsetGatewayCannotBootstrapHumanIdentity() throws Exception {
        long before=jdbc.sql("SELECT count(*) FROM users").query(Long.class).single();
        mvc.perform(get("/api/v1/me").header("X-Mulino-Local-Role","MANAGER")
                .header("X-Mulino-Local-Human","any-key")).andExpect(status().isUnauthorized());
        assertThat(jdbc.sql("SELECT count(*) FROM users").query(Long.class).single()).isEqualTo(before);
    }
}

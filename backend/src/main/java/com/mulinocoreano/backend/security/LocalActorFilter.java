package com.mulinocoreano.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** local 프로필의 인간 API에만 설치한다. 역할 헤더는 외부 인증이 아니다. */
public class LocalActorFilter extends OncePerRequestFilter {
    public static final String ROLE_HEADER = "X-Mulino-Local-Role";
    public static final String HUMAN_HEADER = "X-Mulino-Local-Human";
    private final LocalActorDirectory directory;
    private final LocalAuthProperties properties;

    public LocalActorFilter(LocalActorDirectory directory, LocalAuthProperties properties) {
        this.directory = directory; this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getHeader("Authorization") != null || request.getHeader("X-Mulino-Local-Service") != null) {
            response.setStatus(401);
            return;
        }
        String secret = request.getHeader(HUMAN_HEADER);
        if (secret == null || properties.humanSecret() == null
                || !java.security.MessageDigest.isEqual(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        properties.humanSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            response.setStatus(401); return;
        }
        String role = request.getHeader(ROLE_HEADER);
        if (role == null || role.isBlank()) {
            response.setStatus(401); return;
        }
        HumanActor actor;
        try {
            actor = directory.humanForRole(role);
        } catch (org.springframework.web.server.ResponseStatusException inactive) {
            response.setStatus(inactive.getStatusCode().value()); return;
        } catch (IllegalArgumentException invalidRole) {
            response.setStatus(400);
            return;
        }
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new ActorAuthenticationToken(actor));
        SecurityContextHolder.setContext(context);
        try { chain.doFilter(request, response); }
        finally { SecurityContextHolder.clearContext(); }
    }
}

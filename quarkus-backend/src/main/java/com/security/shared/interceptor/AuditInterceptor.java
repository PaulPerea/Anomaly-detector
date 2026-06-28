package com.security.shared.interceptor;

import com.security.application.service.AuditService;
import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Provider
@Priority(2000)
public class AuditInterceptor implements ContainerRequestFilter, ContainerResponseFilter {

    // Rutas que NO se auditan
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/auth/login", "/auth/register", "/q/health"
    );

    // IPs simuladas para demo
    private static final String[] DEMO_IPS = {
            "192.168.1.10", "192.168.1.25", "10.0.0.5",
            "172.16.0.50", "203.0.113.42", "198.51.100.7"
    };

    private static final Random RANDOM = new Random();

    @Inject
    JsonWebToken jwt;

    @Inject
    AuditService auditService;

    // Guarda datos del request para usarlos en el response filter
    private static final ThreadLocal<RequestContext> CTX = new ThreadLocal<>();

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        String path = req.getUriInfo().getPath();

        if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
            return;
        }

        // Determina el userId desde el JWT (si existe)
        Long userId = null;
        try {
            String subject = jwt.getSubject();
            if (subject != null) userId = Long.parseLong(subject);
        } catch (Exception ignored) {}

        CTX.set(new RequestContext(
                userId,
                randomIp(),
                path,
                req.getMethod()
        ));
    }

    @Override
    public void filter(ContainerRequestContext req,
                       ContainerResponseContext res) throws IOException {
        RequestContext ctx = CTX.get();
        if (ctx == null) return;

        try {
            boolean success = res.getStatus() < 400;
            auditService.record(
                    ctx.userId(), ctx.ipAddress(), ctx.resource(),
                    ctx.method(), success, res.getStatus()
            );
        } catch (Exception e) {
            Log.warnf("Error registrando audit log: %s", e.getMessage());
        } finally {
            CTX.remove();
        }
    }

    private String randomIp() {
        return DEMO_IPS[RANDOM.nextInt(DEMO_IPS.length)];
    }

    private record RequestContext(Long userId, String ipAddress,
                                  String resource, String method) {}
}

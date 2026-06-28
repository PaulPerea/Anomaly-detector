package com.security.application.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.security.domain.enums.Role;
import com.security.domain.model.User;
import com.security.shared.dto.Dtos.*;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    private static final int BCRYPT_COST = 12;

    // ─── Login ─────────────────────────────────────────────────────────────────
    @Transactional
    public LoginResponse login(LoginRequest req) {
        User user = User.findByUsername(req.username())
                .orElseThrow(() -> new NotAuthorizedException("Credenciales inválidas"));

        if (!user.active) {
            throw new NotAuthorizedException("Usuario inactivo");
        }

        BCrypt.Result result = BCrypt.verifyer()
                .verify(req.password().toCharArray(), user.passwordHash);

        if (!result.verified) {
            throw new NotAuthorizedException("Credenciales inválidas");
        }

        String token = buildToken(user);
        return new LoginResponse(token, user.username, user.role.name());
    }

    // ─── Register ──────────────────────────────────────────────────────────────
    @Transactional
    public UserResponse register(CreateUserRequest req) {
        if (User.findByUsername(req.username()).isPresent()) {
            throw new BadRequestException("El username ya existe");
        }
        if (User.findByEmail(req.email()).isPresent()) {
            throw new BadRequestException("El email ya está registrado");
        }

        User user = new User();
        user.username = req.username();
        user.email = req.email();
        user.passwordHash = BCrypt.withDefaults().hashToString(BCRYPT_COST, req.password().toCharArray());
        user.role = req.role() != null ? req.role() : Role.USER;
        user.persist();

        return UserResponse.from(user);
    }

    // ─── JWT builder ───────────────────────────────────────────────────────────
    private String buildToken(User user) {
        return Jwt.issuer("anomaly-detector")
                .subject(String.valueOf(user.id))
                .claim("username", user.username)
                .claim("role", user.role.name())
                .groups(Set.of(user.role.name()))
                .expiresIn(Duration.ofHours(8))
                .sign();
    }
}

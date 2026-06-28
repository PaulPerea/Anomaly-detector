package com.security.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.security.domain.enums.RiskLevel;
import com.security.domain.enums.Role;
import com.security.domain.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

// ─── Auth ──────────────────────────────────────────────────────────────────────

public class Dtos {

    public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
    ) {}

    public record LoginResponse(
        String token,
        String username,
        String role
    ) {}

    // ─── Users ─────────────────────────────────────────────────────────────────

    public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Size(min = 6) String password,
        @NotBlank @Email String email,
        Role role
    ) {}

    public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        boolean active,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt
    ) {
        public static UserResponse from(User u) {
            return new UserResponse(u.id, u.username, u.email,
                    u.role.name(), u.active, u.createdAt);
        }
    }

    // ─── Sensitive Data ────────────────────────────────────────────────────────

    public record SensitiveDataResponse(
        Long id,
        String dni,
        String email,
        String phone,
        String accountNumber,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt
    ) {
        public static SensitiveDataResponse from(SensitiveData d) {
            return new SensitiveDataResponse(d.id, d.dni, d.email,
                    d.phone, d.accountNumber, d.createdAt);
        }
    }

    // ─── Access Log ────────────────────────────────────────────────────────────

    public record AccessLogResponse(
        Long id,
        String username,
        String ipAddress,
        String resource,
        String httpMethod,
        int queryCount,
        boolean success,
        int httpStatus,
        String riskLevel,
        String aiReason,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime accessedAt
    ) {
        public static AccessLogResponse from(AccessLog l) {
            return new AccessLogResponse(l.id,
                    l.user != null ? l.user.username : "anonymous",
                    l.ipAddress, l.resource, l.httpMethod,
                    l.queryCount, l.success, l.httpStatus,
                    l.riskLevel != null ? l.riskLevel.name() : "NORMAL",
                    l.aiReason, l.accessedAt);
        }
    }

    // ─── AI ────────────────────────────────────────────────────────────────────

    public record AiAnalysisRequest(
        String userId,
        String ipAddress,
        String resource,
        int queryCount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime accessedAt,
        long failedAttemptsLast10Min,
        boolean isOffHours
    ) {}

    public record AiAnalysisResponse(
        String riskLevel,
        String reason,
        double confidenceScore
    ) {}

    // ─── Alerts ────────────────────────────────────────────────────────────────

    public record AlertResponse(
        Long id,
        String username,
        String riskLevel,
        String reason,
        boolean acknowledged,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        Long accessLogId
    ) {
        public static AlertResponse from(Alert a) {
            return new AlertResponse(a.id,
                    a.user != null ? a.user.username : "unknown",
                    a.riskLevel.name(), a.reason, a.acknowledged,
                    a.createdAt,
                    a.accessLog != null ? a.accessLog.id : null);
        }
    }

    // ─── Generic ───────────────────────────────────────────────────────────────

    public record MessageResponse(String message) {}

    public record ErrorResponse(String error, String detail) {}
}

package com.security.application.service;

import com.security.domain.enums.RiskLevel;
import com.security.domain.model.AccessLog;
import com.security.domain.model.Alert;
import com.security.domain.model.User;
import com.security.infrastructure.client.AiServiceClient;
import com.security.shared.dto.Dtos.*;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@ApplicationScoped
public class AuditService {

    @Inject
    @RestClient
    AiServiceClient aiClient;

    // ─── Persist log + async AI call ───────────────────────────────────────────
    @Transactional
    public AccessLog record(Long userId, String ipAddress, String resource,
                            String httpMethod, boolean success, int httpStatus) {
        AccessLog log = new AccessLog();

        if (userId != null) {
            log.user = User.findById(userId);
        }
        log.ipAddress = ipAddress;
        log.resource = resource;
        log.httpMethod = httpMethod;
        log.success = success;
        log.httpStatus = httpStatus;
        log.queryCount = countRecentQueries(userId);
        log.persist();

        // lanza análisis de IA de forma asíncrona (sin bloquear la respuesta)
        analyzeAsync(log);

        return log;
    }

    private void analyzeAsync(AccessLog log) {
        if (log.user == null) return;

        long userId = log.user.id;
        LocalDateTime tenMinAgo = LocalDateTime.now().minusMinutes(10);
        long failed = AccessLog.countFailedRecentByUser(userId, tenMinAgo);

        boolean offHours = isOffHours(log.accessedAt);

        AiAnalysisRequest aiReq = new AiAnalysisRequest(
                String.valueOf(userId),
                log.ipAddress,
                log.resource,
                log.queryCount,
                log.accessedAt,
                failed,
                offHours
        );

        aiClient.analyze(aiReq)
                .subscribe().with(
                        response -> applyAiResult(log.id, response),
                        error -> Log.warnf("AI service no disponible: %s", error.getMessage())
                );
    }

    // ─── Apply result from AI (in a new transaction) ───────────────────────────
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void applyAiResult(Long logId, AiAnalysisResponse response) {
        AccessLog log = AccessLog.findById(logId);
        if (log == null) return;

        RiskLevel level = parseRiskLevel(response.riskLevel());
        log.riskLevel = level;
        log.aiReason = response.reason();
        log.confidenceScore = response.confidenceScore();
        log.persist();

        if (level == RiskLevel.SUSPICIOUS || level == RiskLevel.HIGH_RISK) {
            createAlert(log, level, response.reason());
        }

        Log.infof("AI result for log#%d: %s (%.2f) — %s",
                logId, level, response.confidenceScore(), response.reason());
    }

    private void createAlert(AccessLog log, RiskLevel level, String reason) {
        Alert alert = new Alert();
        alert.user = log.user;
        alert.accessLog = log;
        alert.riskLevel = level;
        alert.reason = reason;
        alert.persist();
        Log.warnf("ALERT created for user '%s': %s", log.user.username, reason);
    }

    // ─── Queries ───────────────────────────────────────────────────────────────
    public List<AccessLogResponse> getAll() {
        return AccessLog.<AccessLog>listAll().stream()
                .map(AccessLogResponse::from).toList();
    }

    public List<AccessLogResponse> getHighRisk() {
        return AccessLog.findHighRisk().stream()
                .map(AccessLogResponse::from).toList();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────
    private int countRecentQueries(Long userId) {
        if (userId == null) return 1;
        LocalDateTime fiveMinAgo = LocalDateTime.now().minusMinutes(5);
        return (int) AccessLog.countRecentByUser(userId, fiveMinAgo) + 1;
    }

    private boolean isOffHours(LocalDateTime dt) {
        LocalTime t = dt.toLocalTime();
        return t.isBefore(LocalTime.of(8, 0)) || t.isAfter(LocalTime.of(18, 0));
    }

    private RiskLevel parseRiskLevel(String raw) {
        try {
            return RiskLevel.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            return RiskLevel.NORMAL;
        }
    }
}

package com.security.domain.model;

import com.security.domain.enums.RiskLevel;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "access_log", indexes = {
    @Index(name = "idx_log_user", columnList = "user_id"),
    @Index(name = "idx_log_accessed_at", columnList = "accessed_at")
})
public class AccessLog extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User user;

    @Column(name = "ip_address", length = 45)
    public String ipAddress;

    @Column(nullable = false, length = 200)
    public String resource;

    @Column(name = "http_method", length = 10)
    public String httpMethod;

    @Column(name = "query_count")
    public int queryCount;

    public boolean success;

    @Column(name = "http_status")
    public int httpStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    public RiskLevel riskLevel = RiskLevel.NORMAL;

    @Column(name = "ai_reason", length = 500)
    public String aiReason;

    @Column(name = "confidence_score")
    public Double confidenceScore;

    @Column(name = "accessed_at", updatable = false)
    public LocalDateTime accessedAt;

    @PrePersist
    void prePersist() {
        accessedAt = LocalDateTime.now();
    }

    // ─── Queries ───────────────────────────────────────────────────────────────
    public static long countRecentByUser(Long userId, LocalDateTime since) {
        return count("user.id = ?1 and accessedAt >= ?2", userId, since);
    }

    public static long countFailedRecentByUser(Long userId, LocalDateTime since) {
        return count("user.id = ?1 and success = false and accessedAt >= ?2", userId, since);
    }

    public static List<AccessLog> findByUser(Long userId) {
        return list("user.id = ?1 order by accessedAt desc", userId);
    }

    public static List<AccessLog> findHighRisk() {
        return list("riskLevel in (?1, ?2) order by accessedAt desc",
                RiskLevel.SUSPICIOUS, RiskLevel.HIGH_RISK);
    }
}

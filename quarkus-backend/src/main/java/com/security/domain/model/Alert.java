package com.security.domain.model;

import com.security.domain.enums.RiskLevel;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "alerts")
public class Alert extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_log_id")
    public AccessLog accessLog;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    public RiskLevel riskLevel;

    @Column(nullable = false, length = 500)
    public String reason;

    public boolean acknowledged;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "acknowledged_at")
    public LocalDateTime acknowledgedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        acknowledged = false;
    }

    public static List<Alert> findAllOrderedByDate() {
        return list("order by createdAt desc");
    }

    public static List<Alert> findPending() {
        return list("acknowledged = false order by createdAt desc");
    }

    public static List<Alert> findByUser(Long userId) {
        return list("user.id = ?1 order by createdAt desc", userId);
    }
}

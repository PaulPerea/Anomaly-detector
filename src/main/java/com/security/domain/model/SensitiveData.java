package com.security.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sensitive_data")
public class SensitiveData extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    public User owner;

    @Column(length = 8)
    public String dni;

    @Column(length = 150)
    public String email;

    @Column(length = 20)
    public String phone;

    @Column(name = "account_number", length = 20)
    public String accountNumber;

    @Column(updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public static List<SensitiveData> findByOwner(Long ownerId) {
        return list("owner.id", ownerId);
    }
}

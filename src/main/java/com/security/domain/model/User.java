package com.security.domain.model;

import com.security.domain.enums.Role;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    @Column(nullable = false, unique = true, length = 100)
    public String username;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false, unique = true, length = 150)
    public String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Role role;

    public boolean active;

    @Column(updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (role == null) role = Role.USER;
        active = true;
    }

    // ─── Queries ───────────────────────────────────────────────────────────────
    public static Optional<User> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public static Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}

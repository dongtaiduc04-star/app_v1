package com.getlinkdtd.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_credentials")
public class PasswordCredential {
    @Id
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected PasswordCredential() {
    }

    public PasswordCredential(UUID userId, String passwordHash) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.changedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getChangedAt() { return changedAt; }

    public void changePassword(String passwordHash, Instant now) {
        this.passwordHash = passwordHash;
        this.changedAt = now;
    }
}

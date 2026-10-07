package com.getlinkdtd.auth.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "username", nullable = false, length = 30)
    private String username;

    @Column(name = "email", length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_login_attempts", nullable = false, columnDefinition = "INT UNSIGNED")
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "purge_after")
    private Instant purgeAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 32)
    private Set<RoleName> roles = new HashSet<>();

    protected User() {
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.roles.add(RoleName.USER);
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public UserStatus getStatus() { return status; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getDeletedAt() { return deletedAt; }
    public Instant getPurgeAfter() { return purgeAfter; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<RoleName> getRoles() { return Set.copyOf(roles); }

    public boolean canAuthenticate(Instant now) {
        if (status == UserStatus.ACTIVE) {
            return true;
        }
        if (status == UserStatus.LOCKED && lockedUntil != null && !lockedUntil.isAfter(now)) {
            status = UserStatus.ACTIVE;
            failedLoginAttempts = 0;
            lockedUntil = null;
            updatedAt = now;
            return true;
        }
        return false;
    }

    public void recordSuccessfulLogin(Instant now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    public void recordFailedLogin(int maximumAttempts, Instant lockedUntil, Instant now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maximumAttempts) {
            status = UserStatus.LOCKED;
            this.lockedUntil = lockedUntil;
        }
        updatedAt = now;
    }

    public boolean canResetPassword() {
        return status == UserStatus.ACTIVE || status == UserStatus.LOCKED;
    }

    public void restoreAuthenticationAfterPasswordReset(Instant now) {
        status = UserStatus.ACTIVE;
        failedLoginAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }
}

package com.getlinkdtd.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "getlink.auth.password-reset")
public record PasswordResetProperties(Duration ttl, boolean logToken, String frontendBaseUrl) {
    public PasswordResetProperties {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Password reset TTL must be positive");
        }
        if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
            throw new IllegalArgumentException("Frontend base URL must be configured");
        }
        frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }
}

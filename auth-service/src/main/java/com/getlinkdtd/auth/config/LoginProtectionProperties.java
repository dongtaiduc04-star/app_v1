package com.getlinkdtd.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "getlink.auth.login")
public record LoginProtectionProperties(int maxFailedAttempts, Duration lockDuration) {
    public LoginProtectionProperties {
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException("Maximum failed login attempts must be positive");
        }
        if (lockDuration == null || lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalArgumentException("Login lock duration must be positive");
        }
    }
}

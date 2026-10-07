package com.getlinkdtd.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "getlink.auth.refresh")
public record RefreshTokenProperties(
        Duration ttl,
        String cookieName,
        String cookiePath,
        boolean cookieSecure) {
    public RefreshTokenProperties {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Refresh token TTL must be positive");
        }
        if (cookieName == null || cookieName.isBlank()) {
            throw new IllegalArgumentException("Refresh cookie name must be configured");
        }
        if (cookiePath == null || !cookiePath.startsWith("/")) {
            throw new IllegalArgumentException("Refresh cookie path must be absolute");
        }
    }
}

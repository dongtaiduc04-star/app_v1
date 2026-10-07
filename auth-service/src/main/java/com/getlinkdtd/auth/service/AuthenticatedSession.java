package com.getlinkdtd.auth.service;

import com.getlinkdtd.auth.web.dto.AuthTokenResponse;

import java.time.Instant;

public record AuthenticatedSession(
        AuthTokenResponse access,
        String refreshToken,
        Instant refreshExpiresAt) {
    @Override
    public String toString() {
        return "AuthenticatedSession[access=" + access
                + ", refreshToken=<redacted>, refreshExpiresAt=" + refreshExpiresAt + "]";
    }
}

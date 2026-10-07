package com.getlinkdtd.auth.service;

import com.getlinkdtd.auth.config.RefreshTokenProperties;
import com.getlinkdtd.auth.domain.RefreshSession;
import com.getlinkdtd.auth.domain.User;
import com.getlinkdtd.auth.repository.RefreshSessionRepository;
import com.getlinkdtd.auth.repository.UserRepository;
import com.getlinkdtd.auth.security.OpaqueTokenService;
import com.getlinkdtd.auth.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshSessionRepository sessions;
    private final UserRepository users;
    private final OpaqueTokenService tokens;
    private final RefreshTokenProperties properties;

    public RefreshTokenService(
            RefreshSessionRepository sessions,
            UserRepository users,
            OpaqueTokenService tokens,
            RefreshTokenProperties properties) {
        this.sessions = sessions;
        this.users = users;
        this.tokens = tokens;
        this.properties = properties;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        return create(user.getId(), UUID.randomUUID(), Instant.now());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public RotatedRefreshToken rotate(String rawToken) {
        Instant now = Instant.now();
        RefreshSession current = sessions.findByTokenHashForUpdate(tokens.hash(rawToken))
                .orElseThrow(RefreshTokenService::invalidRefreshToken);

        if (current.getRevokedAt() != null) {
            sessions.revokeFamily(current.getTokenFamily(), now);
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "REFRESH_TOKEN_REUSED",
                    "Refresh token reuse detected; the session family was revoked");
        }
        if (!current.isActive(now)) {
            current.revoke(now);
            throw invalidRefreshToken();
        }

        User user = users.findById(current.getUserId()).orElseThrow(RefreshTokenService::invalidRefreshToken);
        if (!user.canAuthenticate(now)) {
            sessions.revokeFamily(current.getTokenFamily(), now);
            throw invalidRefreshToken();
        }

        current.revoke(now);
        IssuedRefreshToken replacement = create(user.getId(), current.getTokenFamily(), now);
        return new RotatedRefreshToken(user, replacement);
    }

    @Transactional
    public void revokeCurrent(String rawToken, UUID expectedUserId) {
        sessions.findByTokenHashForUpdate(tokens.hash(rawToken))
                .filter(session -> session.getUserId().equals(expectedUserId))
                .ifPresent(session -> session.revoke(Instant.now()));
    }

    @Transactional
    public void revokeAll(UUID userId) {
        sessions.revokeAllForUser(userId, Instant.now());
    }

    private IssuedRefreshToken create(UUID userId, UUID family, Instant now) {
        OpaqueTokenService.IssuedOpaqueToken token = tokens.issue();
        Instant expiresAt = now.plus(properties.ttl());
        sessions.save(new RefreshSession(userId, family, token.hash(), expiresAt));
        return new IssuedRefreshToken(token.value(), expiresAt);
    }

    private static ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
    }

    public record IssuedRefreshToken(String value, Instant expiresAt) {
        @Override
        public String toString() {
            return "IssuedRefreshToken[value=<redacted>, expiresAt=" + expiresAt + "]";
        }
    }

    public record RotatedRefreshToken(User user, IssuedRefreshToken token) {
    }
}

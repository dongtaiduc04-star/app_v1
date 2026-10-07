package com.getlinkdtd.auth.service;

import com.getlinkdtd.auth.config.PasswordResetProperties;
import com.getlinkdtd.auth.domain.PasswordCredential;
import com.getlinkdtd.auth.domain.PasswordResetToken;
import com.getlinkdtd.auth.domain.User;
import com.getlinkdtd.auth.repository.PasswordCredentialRepository;
import com.getlinkdtd.auth.repository.PasswordResetTokenRepository;
import com.getlinkdtd.auth.repository.UserRepository;
import com.getlinkdtd.auth.security.OpaqueTokenService;
import com.getlinkdtd.auth.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordCredentialRepository passwords;
    private final PasswordResetTokenRepository resetTokens;
    private final OpaqueTokenService tokens;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final PasswordResetNotifier notifier;
    private final PasswordResetProperties properties;
    private final RefreshTokenService refreshTokens;

    public PasswordResetService(
            UserRepository users,
            PasswordCredentialRepository passwords,
            PasswordResetTokenRepository resetTokens,
            OpaqueTokenService tokens,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            PasswordResetNotifier notifier,
            PasswordResetProperties properties,
            RefreshTokenService refreshTokens) {
        this.users = users;
        this.passwords = passwords;
        this.resetTokens = resetTokens;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.notifier = notifier;
        this.properties = properties;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public void request(String identifier) {
        findUser(identifier).filter(User::canResetPassword).ifPresent(user -> {
            OpaqueTokenService.IssuedOpaqueToken token = tokens.issue();
            resetTokens.save(new PasswordResetToken(
                    user.getId(), token.hash(), Instant.now().plus(properties.ttl())));
            notifier.send(user, token.value());
        });
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        passwordPolicy.validate(newPassword);
        Instant now = Instant.now();
        PasswordResetToken resetToken = resetTokens.findByTokenHashForUpdate(tokens.hash(rawToken))
                .orElseThrow(PasswordResetService::invalidToken);
        if (!resetToken.isActive(now)) {
            throw invalidToken();
        }

        User user = users.findById(resetToken.getUserId()).orElseThrow(PasswordResetService::invalidToken);
        if (!user.canResetPassword()) {
            throw invalidToken();
        }
        PasswordCredential credential = passwords.findById(user.getId())
                .orElseThrow(PasswordResetService::invalidToken);

        credential.changePassword(passwordEncoder.encode(newPassword), now);
        resetToken.markUsed(now);
        user.restoreAuthenticationAfterPasswordReset(now);
        refreshTokens.revokeAll(user.getId());
    }

    private Optional<User> findUser(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String normalized = identifier.strip().toLowerCase(Locale.ROOT);
        return normalized.contains("@")
                ? users.findByEmailIgnoreCase(normalized)
                : users.findByUsernameIgnoreCase(normalized);
    }

    private static ApiException invalidToken() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PASSWORD_RESET_TOKEN",
                "Password reset token is invalid or expired");
    }
}

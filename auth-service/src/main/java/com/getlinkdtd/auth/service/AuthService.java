package com.getlinkdtd.auth.service;

import com.getlinkdtd.auth.domain.PasswordCredential;
import com.getlinkdtd.auth.domain.User;
import com.getlinkdtd.auth.config.LoginProtectionProperties;
import com.getlinkdtd.auth.repository.PasswordCredentialRepository;
import com.getlinkdtd.auth.repository.UserRepository;
import com.getlinkdtd.auth.security.JwtService;
import com.getlinkdtd.auth.web.ApiException;
import com.getlinkdtd.auth.web.dto.AccountResponse;
import com.getlinkdtd.auth.web.dto.AuthTokenResponse;
import com.getlinkdtd.auth.web.dto.LoginRequest;
import com.getlinkdtd.auth.web.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9-]{3,30}$");
    private static final Set<String> RESERVED_USERNAMES = Set.of(
            "admin", "api", "login", "register", "dashboard", "settings", "health", "system-status");

    private final UserRepository users;
    private final PasswordCredentialRepository passwords;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final LoginProtectionProperties loginProtection;

    public AuthService(
            UserRepository users,
            PasswordCredentialRepository passwords,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            JwtService jwtService,
            RefreshTokenService refreshTokens,
            LoginProtectionProperties loginProtection) {
        this.users = users;
        this.passwords = passwords;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.loginProtection = loginProtection;
    }

    @Transactional
    public AuthenticatedSession register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        validateUsername(username);
        passwordPolicy.validate(request.password());
        String email = normalizeEmail(request.email());

        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Username is already in use");
        }
        if (email != null && users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email is already in use");
        }

        User user = users.saveAndFlush(new User(username, email));
        passwords.save(new PasswordCredential(user.getId(), passwordEncoder.encode(request.password())));
        return authenticatedSession(user, refreshTokens.issue(user));
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthenticatedSession login(LoginRequest request) {
        String username = normalizeUsername(request.username());
        User user = users.findByUsernameIgnoreCase(username).orElseThrow(AuthService::invalidCredentials);
        PasswordCredential credential = passwords.findById(user.getId()).orElseThrow(AuthService::invalidCredentials);
        Instant now = Instant.now();

        if (!user.canAuthenticate(now)) {
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            user.recordFailedLogin(
                    loginProtection.maxFailedAttempts(),
                    now.plus(loginProtection.lockDuration()),
                    now);
            throw invalidCredentials();
        }

        user.recordSuccessfulLogin(now);
        return authenticatedSession(user, refreshTokens.issue(user));
    }

    public AuthenticatedSession refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokens.rotate(rawRefreshToken);
        return authenticatedSession(rotated.user(), rotated.token());
    }

    public void logoutCurrent(UUID userId, String rawRefreshToken) {
        refreshTokens.revokeCurrent(rawRefreshToken, userId);
    }

    public void logoutAll(UUID userId) {
        refreshTokens.revokeAll(userId);
    }

    @Transactional(readOnly = true)
    public AccountResponse currentAccount(UUID userId) {
        return users.findById(userId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
    }

    private AuthTokenResponse tokenResponse(User user) {
        JwtService.IssuedAccessToken token = jwtService.issue(user);
        return new AuthTokenResponse(
                token.value(), "Bearer", token.expiresInSeconds(), AccountResponse.from(user));
    }

    private AuthenticatedSession authenticatedSession(
            User user, RefreshTokenService.IssuedRefreshToken refreshToken) {
        return new AuthenticatedSession(tokenResponse(user), refreshToken.value(), refreshToken.expiresAt());
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static void validateUsername(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches() || RESERVED_USERNAMES.contains(username)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_USERNAME", "Username is invalid or reserved");
        }
    }

    private static InvalidCredentialsException invalidCredentials() {
        return new InvalidCredentialsException();
    }
}

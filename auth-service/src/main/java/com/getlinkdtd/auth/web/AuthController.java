package com.getlinkdtd.auth.web;

import com.getlinkdtd.auth.service.AuthService;
import com.getlinkdtd.auth.service.PasswordResetService;
import com.getlinkdtd.auth.web.dto.AccountResponse;
import com.getlinkdtd.auth.web.dto.AuthTokenResponse;
import com.getlinkdtd.auth.web.dto.LoginRequest;
import com.getlinkdtd.auth.web.dto.RegisterRequest;
import com.getlinkdtd.auth.web.dto.ForgotPasswordRequest;
import com.getlinkdtd.auth.web.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService authService;
    private final RefreshCookieService refreshCookies;
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            RefreshCookieService refreshCookies,
            PasswordResetService passwordResetService) {
        this.authService = authService;
        this.refreshCookies = refreshCookies;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/auth/register")
    ResponseEntity<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return sessionResponse(HttpStatus.CREATED, authService.register(request));
    }

    @PostMapping("/auth/login")
    ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return sessionResponse(HttpStatus.OK, authService.login(request));
    }

    @PostMapping("/auth/refresh")
    ResponseEntity<AuthTokenResponse> refresh(HttpServletRequest request) {
        return sessionResponse(HttpStatus.OK, authService.refresh(refreshCookies.require(request)));
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logoutCurrent(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        refreshCookies.read(request).ifPresent(token ->
                authService.logoutCurrent(UUID.fromString(jwt.getSubject()), token));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }

    @PostMapping("/auth/logout-all")
    ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        authService.logoutAll(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }

    @PostMapping("/auth/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.request(request.identifier());
    }

    @PostMapping("/auth/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.newPassword());
    }

    @GetMapping("/me")
    AccountResponse currentAccount(@AuthenticationPrincipal Jwt jwt) {
        return authService.currentAccount(UUID.fromString(jwt.getSubject()));
    }

    private ResponseEntity<AuthTokenResponse> sessionResponse(
            HttpStatus status, com.getlinkdtd.auth.service.AuthenticatedSession session) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookies.create(session.refreshToken()).toString())
                .body(session.access());
    }
}

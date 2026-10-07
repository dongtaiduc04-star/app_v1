package com.getlinkdtd.auth.web;

import com.getlinkdtd.auth.config.RefreshTokenProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class RefreshCookieService {
    private final RefreshTokenProperties properties;

    public RefreshCookieService(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> properties.cookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    public String require(HttpServletRequest request) {
        return read(request).orElseThrow(() -> new ApiException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "REFRESH_COOKIE_REQUIRED",
                "Refresh cookie is required"));
    }

    public ResponseCookie create(String token) {
        return cookie(token, properties.ttl());
    }

    public ResponseCookie clear() {
        return cookie("", Duration.ZERO);
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(properties.cookiePath())
                .maxAge(maxAge)
                .build();
    }
}

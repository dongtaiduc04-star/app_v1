package com.getlinkdtd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getlinkdtd.auth.repository.PasswordCredentialRepository;
import com.getlinkdtd.auth.repository.PasswordResetTokenRepository;
import com.getlinkdtd.auth.repository.RefreshSessionRepository;
import com.getlinkdtd.auth.repository.UserRepository;
import com.getlinkdtd.auth.domain.PasswordResetToken;
import com.getlinkdtd.auth.domain.UserStatus;
import com.getlinkdtd.auth.security.OpaqueTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthServiceApplicationTests {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("authdb");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("getlink.auth.jwt.secret", () -> "test-only-secret-with-at-least-32-bytes");
        registry.add("getlink.auth.login.max-failed-attempts", () -> "3");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired PasswordCredentialRepository passwords;
    @Autowired RefreshSessionRepository refreshSessions;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired OpaqueTokenService opaqueTokens;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired MockMvc mvc;

    @Test
    void kubernetesHealthProbesArePublic() throws Exception {
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @Test
    void flywayCreatesSchemaAndPasswordAuthenticationIssuesUsableJwt() throws Exception {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                        + "AND table_name IN ('users','user_roles','password_credentials',"
                        + "'refresh_sessions','password_reset_tokens')",
                Integer.class);

        assertThat(count).isEqualTo(5);
        assertThat(passwords.count()).isZero();
        assertThat(refreshSessions.count()).isZero();
        assertThat(resetTokens.count()).isZero();

        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongIssuerToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        String registerBody = """
                {"username":"alice-link","email":"alice@example.com","password":"Strong-pass-123!"}
                """;
        MvcResult registerResult = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.account.username").value("alice-link"))
                .andExpect(jsonPath("$.account.roles[0]").value("USER"))
                .andReturn();

        String registerResponse = registerResult.getResponse().getContentAsString();
        Cookie originalRefreshCookie = refreshCookie(registerResult);
        assertThat(originalRefreshCookie.isHttpOnly()).isTrue();
        assertThat(originalRefreshCookie.getSecure()).isTrue();
        assertThat(originalRefreshCookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(refreshSessions.findAll()).singleElement().satisfies(session -> {
            assertThat(session.getTokenHash()).hasSize(64);
            assertThat(session.getTokenHash()).isNotEqualTo(originalRefreshCookie.getValue());
        });

        String registeredAccessToken = com.jayway.jsonpath.JsonPath.read(registerResponse, "$.accessToken");
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + registeredAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        MvcResult refreshResult = mvc.perform(post("/api/v1/auth/refresh").cookie(originalRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie rotatedRefreshCookie = refreshCookie(refreshResult);
        assertThat(rotatedRefreshCookie.getValue()).isNotEqualTo(originalRefreshCookie.getValue());

        mvc.perform(post("/api/v1/auth/refresh").cookie(originalRefreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));
        mvc.perform(post("/api/v1/auth/refresh").cookie(rotatedRefreshCookie))
                .andExpect(status().isUnauthorized());

        MvcResult loginResult = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ALICE-LINK\",\"password\":\"Strong-pass-123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        Cookie loginRefreshCookie = refreshCookie(loginResult);
        String loginAccessToken = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.accessToken");
        mvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken)
                        .cookie(loginRefreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertThat(refreshCookie(result).getMaxAge()).isZero());
        mvc.perform(post("/api/v1/auth/refresh").cookie(loginRefreshCookie))
                .andExpect(status().isUnauthorized());

        MvcResult deviceOne = login();
        MvcResult deviceTwo = login();
        String deviceOneAccessToken = com.jayway.jsonpath.JsonPath.read(
                deviceOne.getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(post("/api/v1/auth/logout-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + deviceOneAccessToken))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(deviceOne)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(deviceTwo)))
                .andExpect(status().isUnauthorized());

        long resetTokenCount = resetTokens.count();
        mvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"nobody@example.com\"}"))
                .andExpect(status().isAccepted());
        assertThat(resetTokens.count()).isEqualTo(resetTokenCount);

        mvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"alice@example.com\"}"))
                .andExpect(status().isAccepted());
        assertThat(resetTokens.count()).isEqualTo(resetTokenCount + 1);
        assertThat(resetTokens.findAll()).allSatisfy(token ->
                assertThat(token.getTokenHash()).hasSize(64));

        MvcResult sessionBeforeReset = login();
        String rawResetToken = opaqueTokens.issue().value();
        var user = users.findByUsernameIgnoreCase("alice-link").orElseThrow();
        resetTokens.saveAndFlush(new PasswordResetToken(
                user.getId(), opaqueTokens.hash(rawResetToken), Instant.now().plusSeconds(900)));

        String newPassword = "New-strong-pass-456!";
        mvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawResetToken
                                + "\",\"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(sessionBeforeReset)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice-link\",\"password\":\"Strong-pass-123!\"}"))
                .andExpect(status().isUnauthorized());
        login(newPassword);
        mvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawResetToken
                                + "\",\"newPassword\":\"Another-pass-789!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_TOKEN"));

        for (int attempt = 0; attempt < 3; attempt++) {
            mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice-link\",\"password\":\"Wrong-pass-000!\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice-link\",\"password\":\"" + newPassword + "\"}"))
                .andExpect(status().isUnauthorized());
        var lockedUser = users.findByUsernameIgnoreCase("alice-link").orElseThrow();
        assertThat(lockedUser.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(lockedUser.getLockedUntil()).isAfter(Instant.now());

        assertThat(com.jayway.jsonpath.JsonPath.<String>read(loginResponse, "$.accessToken")).isNotBlank();
        assertThat(passwordEncoder.matches(
                "New-strong-pass-456!", passwords.findById(users.findByUsernameIgnoreCase("alice-link").orElseThrow().getId())
                        .orElseThrow().getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(
                "Strong-pass-123!", passwords.findAll().get(0).getPasswordHash())).isFalse();
        assertThat(passwords.findAll().get(0).getPasswordHash())
                .doesNotContain("Strong-pass-123!", "New-strong-pass-456!");
    }

    private MvcResult login() throws Exception {
        return login("Strong-pass-123!");
    }

    private MvcResult login(String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice-link\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static MockCookie refreshCookie(MvcResult result) {
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(header).isNotBlank();
        return MockCookie.parse(header);
    }

    private String wrongIssuerToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("untrusted-issuer")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600))
                .subject(UUID.randomUUID().toString())
                .claim("roles", java.util.List.of("USER"))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}

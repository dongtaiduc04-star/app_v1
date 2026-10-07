package com.getlinkdtd.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.getlinkdtd.link.repository.LinkRepository;
import com.getlinkdtd.link.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LinkServiceApplicationTests {
    private static final String JWT_SECRET = "local-test-secret-that-is-at-least-32-bytes-long";
    private static final Path AVATAR_DIRECTORY;
    static {
        try {
            AVATAR_DIRECTORY = Files.createTempDirectory("getlink-avatar-test-");
        } catch (java.io.IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("linkdb");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("getlink.link.jwt.secret", () -> JWT_SECRET);
        registry.add("getlink.link.jwt.issuer", () -> "getlink_dtd");
        registry.add("getlink.link.avatar.directory", AVATAR_DIRECTORY::toString);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ProfileRepository profiles;
    @Autowired LinkRepository links;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void kubernetesHealthProbesArePublic() throws Exception {
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @Test
    void flywayCreatesLinkSchemaAndRepositoriesUseIt() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                        + "AND table_name IN ('profiles','links')",
                Integer.class);

        assertThat(count).isEqualTo(2);
        assertThat(profiles.count()).isZero();
        assertThat(links.count()).isZero();
    }

    @Test
    void authenticatedUserCanManageLinksAndPublishProfile() throws Exception {
        var owner = jwt().jwt(token -> token
                .subject("7bd8108c-9c85-4f8b-aecd-a9ef98793f2d")
                .claim("username", "ductai")
                .claim("roles", java.util.List.of("USER")));

        mvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mvc.perform(get("/api/v1/me/profile").with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ductai"))
                .andExpect(jsonPath("$.backgroundTheme").value("aurora"))
                .andExpect(jsonPath("$.buttonStyle").value("soft"))
                .andExpect(jsonPath("$.fontFamily").value("system"))
                .andExpect(jsonPath("$.links.length()").value(0));

        mvc.perform(patch("/api/v1/me/profile").with(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Đức Tài","bio":"Builder","backgroundTheme":"midnight","buttonStyle":"pill","fontFamily":"tahoma"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backgroundTheme").value("midnight"))
                .andExpect(jsonPath("$.buttonStyle").value("pill"))
                .andExpect(jsonPath("$.fontFamily").value("tahoma"));

        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
        MockMultipartFile avatar = new MockMultipartFile("file", "avatar.png", "image/png", png);
        String uploaded = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/me/profile/avatar")
                        .file(avatar).with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.startsWith("/api/links/avatars/")))
                .andReturn().getResponse().getContentAsString();
        String avatarUrl = json.readTree(uploaded).path("avatarUrl").asText();
        mvc.perform(get(avatarUrl.replace("/api/links/", "/api/v1/")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));

        mvc.perform(patch("/api/v1/me/profile").with(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Đức Tài","bio":"Updated","avatarUrl":"https://example.com/ignored.png"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(avatarUrl));

        MockMultipartFile invalidAvatar = new MockMultipartFile("file", "avatar.png", "image/png", "not-an-image".getBytes());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/me/profile/avatar")
                        .file(invalidAvatar).with(owner))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AVATAR"));

        String created = mvc.perform(post("/api/v1/me/links").with(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"GitHub","destinationUrl":"https://github.com/example","icon":"github"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(0))
                .andReturn().getResponse().getContentAsString();
        JsonNode link = json.readTree(created);
        String linkId = link.path("id").asText();

        mvc.perform(get("/api/v1/profiles/ductai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[0].title").value("GitHub"));

        mvc.perform(get("/api/v1/r/{linkId}", linkId))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://github.com/example"));

        mvc.perform(post("/api/v1/me/links").with(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Unsafe","destinationUrl":"javascript:alert(1)"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_URL"));

        var anotherUser = jwt().jwt(token -> token
                .subject("a26cb458-8a1f-4fe6-82ae-7fb96b5f3de9")
                .claim("username", "another-user")
                .claim("roles", java.util.List.of("USER")));
        mvc.perform(patch("/api/v1/me/links/{linkId}", linkId).with(anotherUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Changed","destinationUrl":"https://example.com","enabled":true}
                                """))
                .andExpect(status().isNotFound());
    }
}

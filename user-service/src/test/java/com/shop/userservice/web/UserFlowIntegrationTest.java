package com.shop.userservice.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration test against a REAL Postgres in a container. Proves the
 * whole vertical slice — controller, validation, JPA persistence, password
 * hashing and JWT issuance — works together.
 *
 * disabledWithoutDocker=true means the class is skipped (not failed) on machines
 * without a Docker daemon, so the build stays green in constrained environments.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "security.jwt.secret=test-secret-that-is-at-least-32-bytes-long!!",
        "security.jwt.expiration-minutes=60",
        "security.jwt.issuer=user-service-test"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class UserFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;

    @Test
    void register_then_login_then_fetchProfile() throws Exception {
        String registerBody = """
                {"email":"alice@example.com","password":"password123","firstName":"Alice","lastName":"Ng"}
                """;

        // Register -> 201 Created, no password in the response
        String created = mockMvc.perform(post("/users")
                        .contentType("application/json").content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String userId = mapper.readTree(created).get("id").asText();

        // Duplicate registration -> 409 Conflict
        mockMvc.perform(post("/users").contentType("application/json").content(registerBody))
                .andExpect(status().isConflict());

        // Login -> 200 with a bearer token
        String loginBody = """
                {"email":"alice@example.com","password":"password123"}
                """;
        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        JsonNode token = mapper.readTree(loginResponse);
        org.assertj.core.api.Assertions.assertThat(token.get("accessToken").asText()).isNotBlank();

        // Wrong password -> 401
        mockMvc.perform(post("/auth/login").contentType("application/json")
                        .content("""
                                {"email":"alice@example.com","password":"wrongwrong"}
                                """))
                .andExpect(status().isUnauthorized());

        // /users/me with the gateway-supplied identity header -> our profile
        mockMvc.perform(get("/users/me").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void register_rejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/users").contentType("application/json")
                        .content("""
                                {"email":"not-an-email","password":"short","firstName":"","lastName":"X"}
                                """))
                .andExpect(status().isBadRequest());
    }
}

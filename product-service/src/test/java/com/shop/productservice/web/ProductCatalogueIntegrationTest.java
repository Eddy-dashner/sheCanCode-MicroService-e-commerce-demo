package com.shop.productservice.web;

import com.shop.productservice.client.UserClient;
import com.shop.productservice.client.UserSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catalogue CRUD against a real Postgres. The Feign UserClient is mocked so this
 * test doesn't need user-service running — it isolates product-service's own slice.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ProductCatalogueIntegrationTest {

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

    @MockBean
    UserClient userClient; // stand-in for the sync call to user-service

    @Test
    void create_then_fetch_and_price() throws Exception {
        UUID creator = UUID.randomUUID();
        String body = """
                {"name":"Coffee Mug","description":"350ml","price":12.50}
                """;

        String created = mockMvc.perform(post("/products")
                        .header("X-User-Id", creator.toString())
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Coffee Mug"))
                .andExpect(jsonPath("$.createdBy").value(creator.toString()))
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        // Price endpoint returns the authoritative price
        mockMvc.perform(get("/products/{id}/price", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(12.50));

        // Creator endpoint uses the (mocked) sync call
        when(userClient.getUser(any(UUID.class)))
                .thenReturn(new UserSummary(creator, "maker@example.com", "Mo", "Maker"));
        mockMvc.perform(get("/products/{id}/creator", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Mo"));
    }

    @Test
    void create_rejectsNonPositivePrice() throws Exception {
        mockMvc.perform(post("/products")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content("""
                                {"name":"Bad","price":0}
                                """))
                .andExpect(status().isBadRequest());
    }
}

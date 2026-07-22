package com.shop.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: proves the Spring context wires up (config-server enabled,
 * actuator present) without needing any external service running.
 */
@SpringBootTest
class ConfigServerApplicationTests {
    @Test
    void contextLoads() {
    }
}

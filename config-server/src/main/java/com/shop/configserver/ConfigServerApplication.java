package com.shop.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * The config-server is the first thing to boot. It has NO dependency on the
 * discovery-server: config must be available before anything else can register.
 * That is why it uses a fixed port instead of being discovered.
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}

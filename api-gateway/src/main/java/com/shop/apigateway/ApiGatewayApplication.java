package com.shop.apigateway;

import com.shop.apigateway.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * The one door into the system. Clients talk only to the gateway; it looks up
 * the target service in Eureka and load-balances across its instances. Later
 * phases add JWT validation and rate limiting here so those concerns live in
 * exactly one place instead of in every service.
 */
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

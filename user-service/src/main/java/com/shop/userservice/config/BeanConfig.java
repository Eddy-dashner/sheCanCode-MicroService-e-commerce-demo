package com.shop.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeanConfig {

    /**
     * BCrypt: a deliberately slow, salted hash. Slowness is the point — it makes
     * brute-forcing stolen hashes expensive. We use it directly (no Spring
     * Security filter chain) purely to hash and verify passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

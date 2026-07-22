package com.shop.userservice.web;

import com.shop.userservice.domain.User;
import com.shop.userservice.security.JwtService;
import com.shop.userservice.service.UserService;
import com.shop.userservice.web.dto.AuthResponse;
import com.shop.userservice.web.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange email + password for a JWT access token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.email(), request.password());
        String token = jwtService.issueToken(user);
        return AuthResponse.bearer(token, jwtService.expirationSeconds());
    }
}

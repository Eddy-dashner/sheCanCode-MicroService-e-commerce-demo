package com.shop.userservice.web;

import com.shop.userservice.domain.User;
import com.shop.userservice.service.UserService;
import com.shop.userservice.web.dto.RegisterRequest;
import com.shop.userservice.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new customer account (public)")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        User created = userService.register(request);
        var location = uriBuilder.path("/users/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(UserResponse.from(created));
    }

    /**
     * Returns the caller's own profile. The caller's identity is NOT taken from
     * the request body — it comes from the X-User-Id header that the api-gateway
     * sets AFTER it has validated the JWT. The service trusts that header because
     * only the gateway is reachable from outside.
     */
    @GetMapping("/me")
    @Operation(summary = "Get the authenticated caller's profile")
    public UserResponse me(@RequestHeader("X-User-Id") UUID userId) {
        return UserResponse.from(userService.getById(userId));
    }

    /**
     * Look up a user by id. Intended for service-to-service calls (e.g.
     * product-service enriching a product with its creator), reached directly
     * via service discovery rather than through the public gateway.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id (internal, service-to-service)")
    public UserResponse getById(@PathVariable UUID id) {
        return UserResponse.from(userService.getById(id));
    }
}

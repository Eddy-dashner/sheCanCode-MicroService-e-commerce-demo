package com.shop.userservice.web.dto;

import com.shop.userservice.domain.Role;
import com.shop.userservice.domain.User;

import java.util.Set;
import java.util.UUID;

/** Public view of a user. Note: NO password hash is ever exposed. */
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Set<Role> roles
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles()
        );
    }
}

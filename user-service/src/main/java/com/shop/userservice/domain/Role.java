package com.shop.userservice.domain;

/**
 * Coarse-grained roles. Prefixed ROLE_ to match the convention other services'
 * authorization checks expect when they read the "roles" claim from the JWT.
 */
public enum Role {
    ROLE_CUSTOMER,
    ROLE_ADMIN
}

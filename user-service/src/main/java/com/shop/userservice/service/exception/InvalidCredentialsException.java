package com.shop.userservice.service.exception;

/** Thrown on a failed login. Deliberately vague so we don't leak which field was wrong. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}

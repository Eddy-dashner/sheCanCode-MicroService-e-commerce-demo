package com.shop.userservice.service.exception;

/** Thrown when registering with an email that already exists. */
public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String email) {
        super("Email already registered: " + email);
    }
}

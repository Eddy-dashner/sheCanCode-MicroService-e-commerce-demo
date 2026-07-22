package com.shop.userservice.service;

import com.shop.userservice.domain.Role;
import com.shop.userservice.domain.User;
import com.shop.userservice.repository.UserRepository;
import com.shop.userservice.service.exception.EmailAlreadyUsedException;
import com.shop.userservice.service.exception.InvalidCredentialsException;
import com.shop.userservice.service.exception.UserNotFoundException;
import com.shop.userservice.web.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.UUID;

/**
 * The business rules for identity. Controllers stay thin; all the "how" lives here.
 */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        // Cheap guard for a friendly error; the DB unique constraint is the real
        // guarantee against a race between two concurrent registrations.
        if (users.existsByEmail(request.email())) {
            throw new EmailAlreadyUsedException(request.email());
        }
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                EnumSet.of(Role.ROLE_CUSTOMER) // everyone starts as a customer
        );
        return users.save(user);
    }

    /**
     * Verify credentials and return the user. We always run the password check
     * even when the user is missing would be ideal to avoid timing leaks; here we
     * keep it simple and just throw the same vague error for both cases.
     */
    @Transactional(readOnly = true)
    public User authenticate(String email, String rawPassword) {
        User user = users.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return users.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}

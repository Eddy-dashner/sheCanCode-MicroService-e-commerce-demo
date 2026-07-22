package com.shop.userservice.service;

import com.shop.userservice.domain.Role;
import com.shop.userservice.domain.User;
import com.shop.userservice.repository.UserRepository;
import com.shop.userservice.service.exception.EmailAlreadyUsedException;
import com.shop.userservice.service.exception.InvalidCredentialsException;
import com.shop.userservice.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests: the repository and encoder are mocked, so these run fast and
 * assert only the business rules, not persistence.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository users;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    UserService userService;

    private final RegisterRequest request =
            new RegisterRequest("jo@example.com", "password123", "Jo", "Smith");

    @Test
    void register_hashesPassword_andAssignsCustomerRole() {
        when(users.existsByEmail("jo@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.register(request);

        assertThat(saved.getPasswordHash()).isEqualTo("HASHED");
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(saved.getRoles()).containsExactly(Role.ROLE_CUSTOMER);
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(users.existsByEmail("jo@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void authenticate_failsOnWrongPassword() {
        User stored = new User("jo@example.com", "HASHED", "Jo", "Smith",
                EnumSet.of(Role.ROLE_CUSTOMER));
        when(users.findByEmail("jo@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("wrong", "HASHED")).thenReturn(false);

        assertThatThrownBy(() -> userService.authenticate("jo@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void authenticate_failsWhenUserMissing() {
        when(users.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.authenticate("ghost@example.com", "x"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}

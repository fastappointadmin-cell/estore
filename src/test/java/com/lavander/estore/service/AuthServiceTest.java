package com.lavander.estore.service;

import com.lavander.estore.dto.AuthResponse;
import com.lavander.estore.dto.LoginRequest;
import com.lavander.estore.dto.RegisterRequest;
import com.lavander.estore.exception.ConflictException;
import com.lavander.estore.exception.UnauthorizedException;
import com.lavander.estore.model.Role;
import com.lavander.estore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthServiceTest {

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(
            "test-secret-key-that-is-at-least-32-bytes-long-for-hs256", 1000 * 60 * 60);

    private AuthService newAuthService() {
        return new AuthService(userRepository, passwordEncoder, jwtService);
    }

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void registerCreatesAUserRoleAccountAndReturnsAToken() {
        AuthService authService = newAuthService();
        String email = uniqueEmail();

        AuthResponse response = authService.register(new RegisterRequest(email, "parola123", "Ion Popescu"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().email()).isEqualTo(email);
        assertThat(response.user().fullName()).isEqualTo("Ion Popescu");
        assertThat(response.user().role()).isEqualTo(Role.USER);
        assertThat(jwtService.extractRole(response.token())).isEqualTo("USER");
    }

    @Test
    void registeringTheSameEmailTwiceThrowsConflict() {
        AuthService authService = newAuthService();
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, "parola123", "Ion Popescu"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest(email, "other-password", "Alt Nume")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void loginWithCorrectPasswordSucceeds() {
        AuthService authService = newAuthService();
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, "parola123", "Ion Popescu"));

        AuthResponse response = authService.login(new LoginRequest(email, "parola123"));

        assertThat(response.user().email()).isEqualTo(email);
        assertThat(jwtService.isValid(response.token())).isTrue();
    }

    @Test
    void loginWithWrongPasswordThrowsUnauthorized() {
        AuthService authService = newAuthService();
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, "parola123", "Ion Popescu"));

        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "wrong-password")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginWithUnknownEmailThrowsUnauthorized() {
        AuthService authService = newAuthService();

        assertThatThrownBy(() -> authService.login(new LoginRequest(uniqueEmail(), "parola123")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUserReturnsTheMatchingUser() {
        AuthService authService = newAuthService();
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, "parola123", "Ion Popescu"));

        var userDto = authService.getCurrentUser(email);

        assertThat(userDto.email()).isEqualTo(email);
        assertThat(userDto.fullName()).isEqualTo("Ion Popescu");
    }
}

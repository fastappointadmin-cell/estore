package com.lavander.estore.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-that-is-at-least-32-bytes-long-for-hs256", 1000 * 60 * 60);

    @Test
    void generatedTokenRoundTripsEmailAndRole() {
        String token = jwtService.generateToken("ion@example.com", "ADMIN");

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("ion@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void garbageTokenIsNotValid() {
        assertThat(jwtService.isValid("not-a-real-token")).isFalse();
    }

    @Test
    void tokenSignedWithADifferentSecretIsNotValid() {
        JwtService otherService = new JwtService(
                "a-completely-different-secret-key-also-at-least-32-bytes", 1000 * 60 * 60);
        String token = otherService.generateToken("ion@example.com", "USER");

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void expiredTokenIsNotValid() {
        JwtService shortLivedService = new JwtService(
                "test-secret-key-that-is-at-least-32-bytes-long-for-hs256", -1000);
        String token = shortLivedService.generateToken("ion@example.com", "USER");

        assertThat(jwtService.isValid(token)).isFalse();
    }
}

package com.lavander.estore.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Resource groups an ADMIN must own writes to; browsing (GET) on these stays public.
        String[] adminResources = {
                "/api/products/**",
                "/api/product-categories/**",
                "/api/promotion-groups/**",
                "/api/property-definitions/**",
                "/api/tags/**"
        };

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Without an explicit entry point, Spring Security has none configured
                // (httpBasic/formLogin are both disabled) and falls back to 403 even when
                // the real problem is "no credentials at all" — 401 is the correct code.
                // The access-denied handler is set explicitly alongside it so a valid but
                // insufficient-role request still gets 403, not swept into the same 401.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(
                                (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .authorizeHttpRequests(auth -> auth
                        // Spring Boot's error controller is reached via an internal
                        // forward whenever a filter calls response.sendError(...) — if
                        // it isn't permitted, that forward fails its own authorization
                        // check and overwrites the real status with a second one.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**", "/api/product-categories/**",
                                "/api/promotion-groups/**", "/api/property-definitions/**", "/api/tags/**")
                        .permitAll()
                        // A customer submitting a review, not an admin action — must be
                        // declared before the broader admin-only POST rule below.
                        .requestMatchers(HttpMethod.POST, "/api/products/variants/*/reviews").permitAll()
                        .requestMatchers("/api/cart/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        .requestMatchers(HttpMethod.POST, adminResources).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, adminResources).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, adminResources).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

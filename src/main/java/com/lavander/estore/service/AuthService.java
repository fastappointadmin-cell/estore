package com.lavander.estore.service;

import com.lavander.estore.dto.AuthResponse;
import com.lavander.estore.dto.ChangePasswordRequest;
import com.lavander.estore.dto.LoginRequest;
import com.lavander.estore.dto.RegisterRequest;
import com.lavander.estore.dto.UpdateProfileRequest;
import com.lavander.estore.dto.UserDto;
import com.lavander.estore.exception.ConflictException;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.exception.UnauthorizedException;
import com.lavander.estore.model.Role;
import com.lavander.estore.model.User;
import com.lavander.estore.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.fullName(), Role.USER);
        User saved = userRepository.save(user);
        return toAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return toAuthResponse(user);
    }

    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserDto.fromEntity(user);
    }

    public UserDto updateProfile(String email, UpdateProfileRequest request) {
        User user = requireUser(email);
        user.setFullName(request.fullName());
        return UserDto.fromEntity(userRepository.save(user));
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = requireUser(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User requireUser(String email) {
        if (email == null) {
            throw new UnauthorizedException("Must be logged in");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Must be logged in"));
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, UserDto.fromEntity(user));
    }
}

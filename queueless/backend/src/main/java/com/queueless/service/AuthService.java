package com.queueless.service;

import com.queueless.dto.Dtos.AuthResponse;
import com.queueless.dto.Dtos.LoginRequest;
import com.queueless.dto.Dtos.RegisterRequest;
import com.queueless.dto.Dtos.UserView;
import com.queueless.exception.ApiException;
import com.queueless.model.Role;
import com.queueless.model.User;
import com.queueless.repository.UserRepository;
import com.queueless.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists. Sign in instead.");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT); // Admins are never created through public registration.
        users.save(user);
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password."));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password.");
        }
        return toResponse(user);
    }

    public UserView toView(User user) {
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generate(user), toView(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

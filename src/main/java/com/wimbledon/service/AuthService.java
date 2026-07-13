package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.User;
import com.wimbledon.repository.UserRepository;
import com.wimbledon.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.getUsername()))
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        User user = User.builder()
            .name(req.getName())
            .username(req.getUsername())
            .password(encoder.encode(req.getPassword()))
            .role("USER")
            .build();
        userRepo.save(user);
        String token = jwtUtil.generate(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getName(), user.getUsername(), user.getRole());
    }

    public AuthResponse login(AuthRequest req) {
        User user = userRepo.findByUsername(req.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas."));
        if (!encoder.matches(req.getPassword(), user.getPassword()))
            throw new IllegalArgumentException("Credenciales incorrectas 2.");
        String token = jwtUtil.generate(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getName(), user.getUsername(), user.getRole());
    }
}
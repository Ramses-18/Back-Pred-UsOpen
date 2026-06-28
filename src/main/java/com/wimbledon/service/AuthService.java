package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.User;
import com.wimbledon.repository.UserRepository;
import com.wimbledon.security.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
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
        if (userRepo.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("El correo ya está registrado.");
        User user = User.builder()
            .name(req.getName())
            .email(req.getEmail())
            .password(encoder.encode(req.getPassword()))
            .role("USER")
            .build();
        userRepo.save(user);
        String token = jwtUtil.generate(user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole());
    }

    public AuthResponse login(AuthRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas."));
        if (!encoder.matches(req.getPassword(), user.getPassword()))
            throw new IllegalArgumentException("Credenciales incorrectas.");
        String token = jwtUtil.generate(user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole());
    }
}

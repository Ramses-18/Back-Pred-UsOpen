package com.wimbledon.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

     @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        System.out.println("Header recibido: " + header);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            boolean isValid = jwtUtil.validate(token);
            System.out.println("¿Es el token válido?: " + isValid); 
            if (isValid) {
                String email = jwtUtil.extractEmail(token);
                var auth = new UsernamePasswordAuthenticationToken(
                    email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                System.out.println("Header nulo o sin formato Bearer"); 
            }
        }
        chain.doFilter(req, res);
    }
}

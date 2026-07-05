package com.wimbledon.controller;

import com.wimbledon.dto.NotificationSubscriptionRequest;
import com.wimbledon.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> subscribe(
            @Valid @RequestBody NotificationSubscriptionRequest req,
            Authentication auth) {
        try {
            notificationService.subscribe(auth.getName(), req);
            return ResponseEntity.ok(Map.of("status", "suscribed"));
        } catch (Exception e) {
            log.error("[subscribe] error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String endpoint = body.get("endpoint");
        if (endpoint == null) return ResponseEntity.badRequest().body(Map.of("error", "endpoint requerido"));
        notificationService.unsubscribe(auth.getName(), endpoint);
        return ResponseEntity.ok(Map.of("status", "unsubscribed"));
    }
}
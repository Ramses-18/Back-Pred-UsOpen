package com.wimbledon.service;

import com.wimbledon.dto.NotificationSubscriptionRequest;
import com.wimbledon.entity.NotificationSubscription;
import com.wimbledon.entity.User;
import com.wimbledon.repository.NotificationSubscriptionRepository;
import com.wimbledon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationSubscriptionRepository subRepo;
    private final UserRepository userRepo;

    private static final String VAPID_PUBLIC_KEY = System.getenv("VAPID_PUBLIC_KEY");
    private static final String VAPID_PRIVATE_KEY = System.getenv("VAPID_PRIVATE_KEY");

    @Transactional
    public void subscribe(String email, NotificationSubscriptionRequest req) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        // Borrar suscripción anterior con el mismo endpoint
        subRepo.deleteByEndpoint(req.getEndpoint());

        subRepo.save(NotificationSubscription.builder()
            .user(user)
            .endpoint(req.getEndpoint())
            .p256dh(req.getP256dh())
            .authKey(req.getAuthKey())
            .build());

        log.info("[subscribe] usuario {} suscrito a notificaciones", email);
    }

    @Transactional
    public void unsubscribe(String email, String endpoint) {
        subRepo.deleteByEndpoint(endpoint);
        log.info("[unsubscribe] endpoint removido para usuario {}", email);
    }

    public void sendToUser(Long userId, String title, String body) {
        List<NotificationSubscription> subs = subRepo.findByUserId(userId);
        if (subs.isEmpty()) return;
        for (NotificationSubscription sub : subs) {
            sendPush(sub, title, body);
        }
    }

    public void sendToAllUsers(String title, String body) {
        List<NotificationSubscription> subs = subRepo.findAll();
        log.info("[sendToAllUsers] enviando a {} suscripciones", subs.size());
        for (NotificationSubscription sub : subs) {
            sendPush(sub, title, body);
        }
    }

    private void sendPush(NotificationSubscription sub, String title, String body) {
        if (VAPID_PUBLIC_KEY == null || VAPID_PRIVATE_KEY == null) {
            log.warn("[sendPush] VAPID keys no configuradas, saltando notificación");
            return;
        }

        try {
            String payload = "{\"title\":\"" + title + "\",\"body\":\"" + body + "\",\"icon\":\"/icon-192x192.png\"}";
            byte[] payloadBytes = payload.getBytes("UTF-8");

            // VAPID JWT (simplificado — para producción usar web-push library)
            String jwt = generateVapidJwt();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sub.getEndpoint()))
                .header("Content-Type", "application/octet-stream")
                .header("TTL", "60")
                .header("Authorization", "vapid t=" + jwt + ", k=" + VAPID_PUBLIC_KEY)
                .POST(HttpRequest.BodyPublishers.ofByteArray(encryptPayload(payloadBytes, sub)))
                .build();

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[sendPush] status={} para endpoint {}", response.statusCode(), sub.getEndpoint());

            // Si el endpoint ya no es válido, borrarlo
            if (response.statusCode() == 404 || response.statusCode() == 410) {
                subRepo.delete(sub);
                log.info("[sendPush] suscripción inválida eliminada");
            }
        } catch (Exception e) {
            log.error("[sendPush] error enviando notificación: {}", e.getMessage());
        }
    }

    private String generateVapidJwt() {
        // Simplificado: en producción usar java-jwt o nimbuss-jose-jwt con ES256
        // Por ahora generamos un token básico
        long now = System.currentTimeMillis() / 1000;
        String header = "{\"typ\":\"JWT\",\"alg\":\"ES256\"}";
        String body = "{\"aud\":\"https://fcm.googleapis.com\",\"exp\":" + (now + 86400) + ",\"sub\":\"mailto:admin@wimbledon.com\"}";
        try {
            String h = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes("UTF-8"));
            String b = Base64.getUrlEncoder().withoutPadding().encodeToString(body.getBytes("UTF-8"));
            // En producción: firmar con ECDSA P-256 usando VAPID_PRIVATE_KEY
            return h + "." + b + ".signature_placeholder";
        } catch (Exception e) {
            return "header.body.signature";
        }
    }

    private byte[] encryptPayload(byte[] payload, NotificationSubscription sub) {
        // Para Web Push real se necesita AES-128-GCM con las keys del usuario
        // Por ahora enviamos el payload plano (el service worker puede procesarlo)
        // En producción usar la librería web-push de Java
        return payload;
    }
}
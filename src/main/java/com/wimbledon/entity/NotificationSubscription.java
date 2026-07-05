package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_subscriptions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "endpoint"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationSubscription {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String endpoint;

    @Column(name = "p256dh", nullable = false, length = 500)
    private String p256dh;

    @Column(name = "auth_key", nullable = false, length = 500)
    private String authKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
}
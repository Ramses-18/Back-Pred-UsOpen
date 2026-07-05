package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leagues", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"code"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class League {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
}
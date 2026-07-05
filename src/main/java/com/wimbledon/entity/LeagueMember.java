package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "league_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"league_id", "user_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeagueMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    @Builder.Default
    private java.time.LocalDateTime joinedAt = java.time.LocalDateTime.now();
}
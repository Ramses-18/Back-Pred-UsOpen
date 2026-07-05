package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_picks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TournamentPick {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100) private String champion;
    @Column(length = 100) private String semi1;
    @Column(length = 100) private String semi2;
    @Column(length = 100) private String semi3;
    @Column(length = 100) private String semi4;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default private LocalDateTime updatedAt = LocalDateTime.now();
}

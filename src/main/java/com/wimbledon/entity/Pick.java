package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "picks", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","match_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pick {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false, length = 100) private String winner;
    @Column(name = "sets_winner")  private Integer setsWinner;
    @Column(name = "games_winner") private Integer gamesWinner;
    @Column(name = "games_loser")  private Integer gamesLoser;

    @Column(name = "is_correction", nullable = false)
    @Builder.Default private Boolean isCorrection = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default private LocalDateTime updatedAt = LocalDateTime.now();
}

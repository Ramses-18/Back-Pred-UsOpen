package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "match_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class MatchResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false, length = 100)
    private String winner;

    @Column(name = "sets_winner") private Integer setsWinner;
    @Column(name = "games_winner") private Integer gamesWinner;
    @Column(name = "games_loser")  private Integer gamesLoser;

    @Column(name = "entered_at", nullable = false)
    @Builder.Default private LocalDateTime enteredAt = LocalDateTime.now();
}

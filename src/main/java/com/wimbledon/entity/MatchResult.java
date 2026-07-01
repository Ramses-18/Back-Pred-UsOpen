package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

    // Sets individuales del resultado real
    @Column(name = "set1_w") private Integer set1W;
    @Column(name = "set1_l") private Integer set1L;
    @Column(name = "set2_w") private Integer set2W;
    @Column(name = "set2_l") private Integer set2L;
    @Column(name = "set3_w") private Integer set3W;
    @Column(name = "set3_l") private Integer set3L;
    @Column(name = "set4_w") private Integer set4W;
    @Column(name = "set4_l") private Integer set4L;
    @Column(name = "set5_w") private Integer set5W;
    @Column(name = "set5_l") private Integer set5L;

    // String original de la API ej: "6-3 6-4" para referencia
    @Column(name = "game_result", length = 100)
    private String gameResult;

    @Column(name = "entered_at", nullable = false)
    @Builder.Default private LocalDateTime enteredAt = LocalDateTime.now();
}

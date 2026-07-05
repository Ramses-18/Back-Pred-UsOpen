package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Representa un partido del bracket del torneo (no del día a día).
 * El admin carga los jugadores a medida que avanzan las rondas.
 */
@Entity
@Table(name = "bracket_matches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BracketMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ronda: R128, R64, R32, R16, QF, SF, F */
    @Column(name = "round", nullable = false, length = 8)
    private String round;

    /** Posición dentro de la ronda (1-indexed).
     *  Ej: QF position=1 → primer QF, position=2 → segundo QF, etc. */
    @Column(name = "position_in_round", nullable = false)
    private Integer positionInRound;

    @Column(length = 100)
    private String player1;

    @Column(length = 100)
    private String player2;

    /** Ganador (null si todavía no se jugó) */
    @Column(length = 100)
    private String winner;

    /** Score: "6-4, 6-3, 7-5" (string libre) */
    @Column(name = "score_str", length = 100)
    private String scoreStr;

    /** Sets ganados por el winner (2 o 3) */
    @Column(name = "sets_winner")
    private Integer setsWinner;

    /** Sets ganados por el perdedor */
    @Column(name = "sets_loser")
    private Integer setsLoser;

    /** Id del BracketMatch del que sale player1 (en la ronda anterior) */
    @Column(name = "source_match_1")
    private Long sourceMatch1;

    /** Id del BracketMatch del que sale player2 (en la ronda anterior) */
    @Column(name = "source_match_2")
    private Long sourceMatch2;

    /** Status: SCHEDULED, IN_PLAY, FINISHED, WALKOVER */
    @Column(nullable = false, length = 12)
    @Builder.Default
    private String status = "SCHEDULED";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

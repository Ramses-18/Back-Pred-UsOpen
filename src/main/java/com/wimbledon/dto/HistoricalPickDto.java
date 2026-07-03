package com.wimbledon.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

/**
 * DTO para mostrar un pick histórico de un usuario (con resultado real y puntos).
 * Usado por GET /api/leaderboard/{userId}/picks.
 */
@Data @Builder
public class HistoricalPickDto {
    private Long matchId;
    private LocalDate matchDate;
    private String player1;
    private String player2;
    private String round;
    private String court;
    private String matchStatus;          // SCHEDULED | IN_PLAY | FINISHED | WALKOVER | ...

    // Pick del usuario
    private String pickWinner;
    private Integer pickSetsWinner;
    private boolean isCorrection;

    // Resultado real (null si el partido no terminó)
    private String realWinner;
    private Integer realSetsWinner;
    private String realScore;            // "6-4, 6-3, 7-5"

    // Puntos obtenidos en este pick (0 si no terminó o no acertó)
    private int pointsEarned;
}

package com.wimbledon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MatchCreateRequest {
    @NotNull  private LocalDate matchDate;
    private LocalTime matchTime;          // opcional (null = sigue a otro partido)
    @NotBlank private String court;
    @NotBlank private String player1;
    @NotBlank private String player2;
    private String round;
    private Long followsMatchId;          // null si es el 1° de la cancha
}

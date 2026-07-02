package com.wimbledon.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MatchCreateRequest {
    @NotNull  private LocalDate matchDate;
    private LocalTime matchTime;          // AHORA OPCIONAL (null = "sigue a otro partido")
    @NotBlank private String court;
    @NotBlank private String player1;
    @NotBlank private String player2;
    private String round;

    // NUEVOS
    private Long followsMatchId;          // id del partido previo en la misma cancha (null si es el 1°)
    // orderInCourt lo calcula el backend en MatchAdminService.createMatch()
}
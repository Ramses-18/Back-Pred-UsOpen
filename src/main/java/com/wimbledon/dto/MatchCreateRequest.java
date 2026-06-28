package com.wimbledon.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
@Data
public class MatchCreateRequest {
    @NotNull  private LocalDate matchDate;
    @NotNull  private LocalTime matchTime;
    @NotBlank private String court;
    @NotBlank private String player1;
    @NotBlank private String player2;
    private String round;
}

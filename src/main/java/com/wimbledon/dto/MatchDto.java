package com.wimbledon.dto;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
@Data @Builder
public class MatchDto {
    private Long id;
    private LocalDate matchDate;
    private LocalTime matchTime;
    private String court;
    private String player1;
    private String player2;
    private String round;
    private MatchResultDto result;
    private PickDto myPick;
    private boolean deadlinePassed;
}

package com.wimbledon.dto;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private Integer orderInCourt;
    private Long followsMatchId;
    private String status;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private LocalTime estimatedStartTime;

    private boolean deadlineForced;        // NUEVO
    private boolean deadlinePassed;

    private MatchResultDto result;
    private PickDto myPick;
}

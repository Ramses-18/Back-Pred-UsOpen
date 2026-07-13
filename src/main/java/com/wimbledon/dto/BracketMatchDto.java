package com.wimbledon.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class BracketMatchDto {
    private Long id;
    private String round;
    private Integer positionInRound;
    private String player1;
    private String player2;
    private String winner;
    private String scoreStr;
    private Integer setsWinner;
    private Integer setsLoser;
    private Long sourceMatch1;
    private Long sourceMatch2;
    private Long matchId;
    private String status;
}

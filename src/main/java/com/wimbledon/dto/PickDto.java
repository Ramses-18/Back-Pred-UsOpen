package com.wimbledon.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder
public class PickDto {
    private Long matchId;
    private String winner;
    private Integer setsWinner;
    private Integer gamesWinner;
    private Integer gamesLoser;
    private boolean isCorrection;
    private int pointsEarned;
}

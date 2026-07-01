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



    private int set1W;
    private int set1L;
    private int set2W;
    private int set2L;
    private int set3W;
    private int set3L;
    private int set4W;
    private int set4L;
    private int set5W;
    private int set5L;


    
}

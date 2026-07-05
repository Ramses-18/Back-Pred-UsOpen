package com.wimbledon.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class MatchResultDto {
    private String winner;
    private Integer setsWinner;
    private Integer setsLoser;
    private String gameResult;   // "6-3 6-4" string, o "RET" para retiro
    private Boolean retired;



    // Sets individuales del resultado real
    private Integer set1W;
    private Integer set1L;
    private Integer set2W;
    private Integer set2L;
    private Integer set3W;
    private Integer set3L;
    private Integer set4W;
    private Integer set4L;
    private Integer set5W;
    private Integer set5L;
}

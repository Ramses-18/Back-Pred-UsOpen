package com.wimbledon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PickRequest {
    @NotBlank private String winner;
    private Integer setsWinner;
    private Integer gamesWinner;
    private Integer gamesLoser;
    private boolean useCorrection;

    // Sets individuales
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

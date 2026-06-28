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
}

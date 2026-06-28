package com.wimbledon.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder
public class LeaderboardEntryDto {
    private int rank;
    private String name;
    private String email;
    private int totalPoints;
    private int dailyPoints;
    private int tournamentPoints;
    private boolean correctionUsedToday;
}

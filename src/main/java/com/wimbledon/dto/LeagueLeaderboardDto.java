package com.wimbledon.dto;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeagueLeaderboardDto {
    private Long leagueId;
    private String leagueName;
    private String leagueCode;
    private List<LeaderboardEntryDto> members;
}
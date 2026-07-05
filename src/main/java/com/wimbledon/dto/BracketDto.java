package com.wimbledon.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class BracketDto {
    private List<BracketMatchDto> matches;
    private String champion;
    private List<RoundInfo> rounds;

    @Data @Builder
    public static class RoundInfo {
        private String key;        // R128, R64, ...
        private String label;      // "Primera ronda", "Cuartos", ...
        private Integer count;     // cantidad de partidos en esa ronda
    }
}

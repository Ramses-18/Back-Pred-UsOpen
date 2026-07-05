package com.wimbledon.dto;
import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data @Builder
public class TournamentPickDto {
    private String champion;
    private List<String> semis;
}

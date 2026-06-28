package com.wimbledon.dto;
import lombok.Data;
import java.util.List;
@Data
public class TournamentPickRequest {
    private String champion;
    private List<String> semis;
}

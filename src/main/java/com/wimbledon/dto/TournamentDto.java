package com.wimbledon.dto;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class TournamentDto {
    private Long id;
    private String name;
    private String slug;
    private boolean active;
}
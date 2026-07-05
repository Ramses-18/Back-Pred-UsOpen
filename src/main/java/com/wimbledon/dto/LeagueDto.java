package com.wimbledon.dto;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class LeagueDto {
    private Long id;
    private String name;
    private String code;
    private String ownerName;
    private int memberCount;
    private boolean isOwner;
    private boolean isMember;
}
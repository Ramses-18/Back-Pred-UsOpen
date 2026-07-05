package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "atp_ranking")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AtpRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String playerName;

    @Column(nullable = false)
    private Integer points;
}
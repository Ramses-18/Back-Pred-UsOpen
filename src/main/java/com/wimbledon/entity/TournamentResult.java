package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_result")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TournamentResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100) private String champion;
    @Column(length = 100) private String semi1;
    @Column(length = 100) private String semi2;
    @Column(length = 100) private String semi3;
    @Column(length = 100) private String semi4;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default private LocalDateTime updatedAt = LocalDateTime.now();
}

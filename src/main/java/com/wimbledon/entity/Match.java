package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "matches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_date", nullable = false)
    private LocalDate matchDate;

    @Column(name = "match_time")
    private LocalTime matchTime;

    @Column(nullable = false, length = 60)
    private String court;

    @Column(nullable = false, length = 100)
    private String player1;

    @Column(nullable = false, length = 100)
    private String player2;

    @Column(length = 40)
    private String round;

    @Column(name = "order_in_court", nullable = false)
    @Builder.Default
    private Integer orderInCourt = 1;

    @Column(name = "follows_match_id")
    private Long followsMatchId;

    @Column(name = "status", nullable = false, length = 12)
    @Builder.Default
    private String status = "SCHEDULED";

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(name = "api_event_id", length = 40)
    private String apiEventId;

    // NUEVO — el admin forzó el cierre de pronóstico
    @Column(name = "deadline_forced", nullable = false)
    @Builder.Default
    private Boolean deadlineForced = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

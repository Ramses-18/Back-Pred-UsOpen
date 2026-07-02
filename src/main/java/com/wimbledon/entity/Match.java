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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_date", nullable = false)
    private LocalDate matchDate;

    @Column(name = "match_time")
    private LocalTime matchTime;

    @Column(nullable = false, length = 60)
    private String court;            // "Centre Court", "Court 1", etc.

    @Column(nullable = false, length = 100)
    private String player1;
    @Column(nullable = false, length = 100)
    private String player2;

    @Column(length = 40)
    private String round;

    @Column(name = "order_in_court", nullable = false)
    private Integer orderInCourt;          // 1°, 2°, 3° partido de esa cancha

    @Column(name = "follows_match_id")
    private Long followsMatchId;           // FK al partido previo en la misma cancha (null si es el 1°)

    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private String status = "SCHEDULED";   // SCHEDULED | IN_PLAY | FINISHED | WALKOVER | SUSPENDED | RETIRED | ABANDONED

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime; 

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;   // se setea cuando status pasa a FINISHED/ABANDONED/...

    @Column(name = "api_event_id", length = 40, unique = true)
    private String apiEventId;             // id del fixture en API-Sports (para matchear en syncs)

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

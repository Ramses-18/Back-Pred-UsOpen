package com.wimbledon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_corrections",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","used_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyCorrection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "used_date", nullable = false) private LocalDate usedDate;
}

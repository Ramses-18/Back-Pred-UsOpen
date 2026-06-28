package com.wimbledon.repository;
import com.wimbledon.entity.DailyCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
public interface DailyCorrectionRepository extends JpaRepository<DailyCorrection, Long> {
    boolean existsByUserIdAndUsedDate(Long userId, LocalDate date);
}

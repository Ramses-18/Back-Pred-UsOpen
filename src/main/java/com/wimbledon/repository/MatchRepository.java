package com.wimbledon.repository;
import com.wimbledon.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByMatchDateOrderByMatchTimeAsc(LocalDate date);
}

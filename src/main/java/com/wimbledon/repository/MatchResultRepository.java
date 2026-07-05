package com.wimbledon.repository;
import com.wimbledon.entity.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
    Optional<MatchResult> findByMatchId(Long matchId);
}

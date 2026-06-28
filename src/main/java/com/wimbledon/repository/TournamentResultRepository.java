package com.wimbledon.repository;
import com.wimbledon.entity.TournamentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TournamentResultRepository extends JpaRepository<TournamentResult, Long> {
    Optional<TournamentResult> findTopByOrderByIdDesc();
}

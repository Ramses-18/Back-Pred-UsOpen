package com.wimbledon.repository;
import com.wimbledon.entity.TournamentPick;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TournamentPickRepository extends JpaRepository<TournamentPick, Long> {
    Optional<TournamentPick> findByUserId(Long userId);
}

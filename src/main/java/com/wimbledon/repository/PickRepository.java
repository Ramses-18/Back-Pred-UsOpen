package com.wimbledon.repository;

import com.wimbledon.entity.Pick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PickRepository extends JpaRepository<Pick, Long> {

    Optional<Pick> findByUserIdAndMatchId(Long userId, Long matchId);

    @Query("SELECT p FROM Pick p WHERE p.match.id = :matchId")
    List<Pick> findByMatchId(Long matchId);

    // FIX BUG 5: usar parámetro date explícito en vez de CURRENT_DATE
    // (que depende del timezone del server DB)
    @Query("SELECT p FROM Pick p WHERE p.user.id = :userId AND p.match.matchDate = :date")
    List<Pick> findPicksByUserIdAndDate(@Param("userId") Long userId, @Param("date") java.time.LocalDate date);

    // FIX BUG 1: traer TODOS los picks del usuario para la tabla acumulada
    List<Pick> findByUserId(Long userId);
}
package com.wimbledon.repository;

import com.wimbledon.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByMatchDateOrderByMatchTimeAsc(LocalDate date);

    List<Match> findAllByOrderByMatchDateAscMatchTimeAscOrderInCourtAsc();

    List<Match> findByMatchDateAndCourtOrderByOrderInCourtAsc(LocalDate date, String court);

    List<Match> findByMatchDateAndStatusIn(LocalDate date, List<String> statuses);

    List<Match> findByMatchDateGreaterThanEqualOrderByMatchDateAscMatchTimeAsc(LocalDate date);

    @Query("""
        SELECT m FROM Match m
        WHERE (m.matchDate >= :today)
           OR (m.status IN ('IN_PLAY','SUSPENDED') AND m.matchDate >= :lookback)
        ORDER BY m.matchDate ASC, m.matchTime ASC
    """)
    List<Match> findUpcomingOrActive(@Param("today") LocalDate today, @Param("lookback") LocalDate lookback);

    Match findByFollowsMatchId(Long followsMatchId);

    @Query(value = "SELECT COALESCE(MAX(order_in_court), 0) FROM matches WHERE match_date = :date AND court = :court",
           nativeQuery = true)
    Integer findMaxOrderInCourt(@Param("date") LocalDate date, @Param("court") String court);
}

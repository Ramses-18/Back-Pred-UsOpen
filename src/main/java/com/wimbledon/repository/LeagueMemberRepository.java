package com.wimbledon.repository;

import com.wimbledon.entity.LeagueMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LeagueMemberRepository extends JpaRepository<LeagueMember, Long> {

    Optional<LeagueMember> findByLeagueIdAndUserId(Long leagueId, Long userId);

    @Query("SELECT lm.user.id FROM LeagueMember lm WHERE lm.league.id = :leagueId")
    List<Long> findUserIdsByLeagueId(@Param("leagueId") Long leagueId);

    List<LeagueMember> findByUserId(Long userId);

    List<LeagueMember> findByLeagueId(Long leagueId);

    boolean existsByLeagueIdAndUserId(Long leagueId, Long userId);

    long countByLeagueId(Long leagueId);
}
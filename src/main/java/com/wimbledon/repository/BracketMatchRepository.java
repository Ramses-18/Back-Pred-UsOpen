package com.wimbledon.repository;

import com.wimbledon.entity.BracketMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BracketMatchRepository extends JpaRepository<BracketMatch, Long> {

    List<BracketMatch> findByRoundOrderByPositionInRoundAsc(String round);

    List<BracketMatch> findAllByOrderByRoundAscPositionInRoundAsc();

    Optional<BracketMatch> findByRoundAndPositionInRound(String round, Integer positionInRound);

    List<BracketMatch> findByRound(String round);
}

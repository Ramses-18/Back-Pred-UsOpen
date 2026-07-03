package com.wimbledon.repository;
import com.wimbledon.entity.Pick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface PickRepository extends JpaRepository<Pick, Long> {
    Optional<Pick> findByUserIdAndMatchId(Long userId, Long matchId);
    @Query("SELECT p FROM Pick p WHERE p.match.id = :matchId")
    List<Pick> findByMatchId(Long matchId);
    @Query("SELECT p FROM Pick p WHERE p.user.id = :userId AND p.match.matchDate = CURRENT_DATE")
    List<Pick> findTodayPicksByUserId(Long userId);
}

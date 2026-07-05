package com.wimbledon.repository;

import com.wimbledon.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {

    Optional<League> findByCode(String code);

    List<League> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    boolean existsByCode(String code);
}
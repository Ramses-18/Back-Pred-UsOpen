package com.wimbledon.repository;

import com.wimbledon.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    Optional<Tournament> findBySlug(String slug);

    Optional<Tournament> findByActiveTrue();
}
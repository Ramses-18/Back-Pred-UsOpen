package com.wimbledon.service;

import com.wimbledon.entity.AtpRanking;
import com.wimbledon.repository.AtpRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AtpRankingService {

    private final AtpRankingRepository repo;

    public List<AtpRanking> getAll() {
        return repo.findAllByOrderByPointsDescIdAsc();
    }

    @Transactional
    public AtpRanking addPlayer(String playerName, Integer points) {
        long count = repo.count();
        if (count >= 100) {
            throw new RuntimeException("Se alcanzó el máximo de 100 jugadores");
        }
        AtpRanking entry = AtpRanking.builder()
                .playerName(playerName.trim())
                .points(points)
                .build();
        return repo.save(entry);
    }

    @Transactional
    public AtpRanking updatePlayer(Long id, String playerName, Integer points) {
        AtpRanking entry = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado"));
        if (playerName != null && !playerName.isBlank()) {
            entry.setPlayerName(playerName.trim());
        }
        if (points != null) {
            entry.setPoints(points);
        }
        return repo.save(entry);
    }

    @Transactional
    public void deletePlayer(Long id) {
        repo.deleteById(id);
    }
}
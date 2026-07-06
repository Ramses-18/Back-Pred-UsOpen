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

    private static final int MAX_PLAYERS = 100;

    private final AtpRankingRepository repo;

    public List<AtpRanking> getAll() {
        return repo.findAllByOrderByPointsDescIdAsc();
    }

    @Transactional
    public AtpRanking add(String playerName, int points) {
        if (repo.count() >= MAX_PLAYERS) {
            throw new RuntimeException("Se alcanzó el máximo de " + MAX_PLAYERS + " jugadores");
        }
        return repo.save(AtpRanking.builder()
                .playerName(playerName)
                .points(points)
                .build());
    }

    @Transactional
    public AtpRanking update(Long id, String playerName, int points) {
        AtpRanking r = repo.findById(id).orElseThrow(() -> new RuntimeException("Jugador no encontrado"));
        r.setPlayerName(playerName);
        r.setPoints(points);
        return repo.save(r);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
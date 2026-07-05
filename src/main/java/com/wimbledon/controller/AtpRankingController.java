package com.wimbledon.controller;

import com.wimbledon.entity.AtpRanking;
import com.wimbledon.service.AtpRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/atp-ranking")
@RequiredArgsConstructor
public class AtpRankingController {

    private final AtpRankingService service;

    @GetMapping
    public ResponseEntity<List<AtpRanking>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<AtpRanking> addPlayer(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("playerName");
        Integer points = ((Number) body.get("points")).intValue();
        return ResponseEntity.ok(service.addPlayer(name, points));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtpRanking> updatePlayer(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("playerName");
        Integer points = body.get("points") != null
                ? ((Number) body.get("points")).intValue()
                : null;
        return ResponseEntity.ok(service.updatePlayer(id, name, points));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        service.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}
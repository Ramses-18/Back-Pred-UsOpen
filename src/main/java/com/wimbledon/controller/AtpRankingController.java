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
    public ResponseEntity<?> add(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("playerName");
            int points = ((Number) body.get("points")).intValue();
            return ResponseEntity.ok(service.add(name, points));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("playerName");
            int points = ((Number) body.get("points")).intValue();
            return ResponseEntity.ok(service.update(id, name, points));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
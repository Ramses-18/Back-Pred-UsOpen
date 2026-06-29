package com.wimbledon.controller;

import com.wimbledon.dto.*;
import com.wimbledon.entity.Match;
import com.wimbledon.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MatchAdminService          matchAdminService;
    private final TournamentPickServiceFacade facade;
    private final TennisApiService           tennisApiService;

    @PostMapping("/matches")
    public ResponseEntity<Match> createMatch(@Valid @RequestBody MatchCreateRequest req) {
        return ResponseEntity.ok(matchAdminService.createMatch(req));
    }

    @PostMapping("/matches/{matchId}/result")
    public ResponseEntity<MatchResultDto> saveResult(
            @PathVariable Long matchId, @RequestBody MatchResultDto dto) {
        return ResponseEntity.ok(matchAdminService.saveResult(matchId, dto));
    }

    @DeleteMapping("/matches/{matchId}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long matchId) {
        matchAdminService.deleteMatch(matchId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tournament/result")
    public ResponseEntity<TournamentPickDto> saveTournamentResult(
            @RequestBody TournamentPickRequest req) {
        return ResponseEntity.ok(facade.saveResult(req));
    }

    /** Fuerza la sincronización con la API de tenis inmediatamente */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncNow() {
        tennisApiService.syncResults();
        return ResponseEntity.ok(Map.of("status", "Sincronización ejecutada"));
    }
}
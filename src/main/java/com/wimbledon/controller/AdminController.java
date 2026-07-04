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

    /** Sincronización manual (ya no se usa automáticamente cada 2 min). */
    @PostMapping("/sync/tomorrow")
    public ResponseEntity<Map<String, String>> syncTomorrow() {
        tennisApiService.syncTomorrowMatches();
        return ResponseEntity.ok(Map.of("status", "Partidos de mañana sincronizados"));
    }

    /** Cambio manual de status (admin inicia/suspende/finaliza partido). */
    @PatchMapping("/matches/{matchId}/status")
    public ResponseEntity<Match> updateMatchStatus(
            @PathVariable Long matchId,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        return ResponseEntity.ok(matchAdminService.updateStatus(matchId, newStatus));
    }

    /**
     * FIX Req 3: Solo cerrar pronóstico sin cambiar status del partido.
     * El admin cierra los pronósticos cuando quiere; el partido sigue SCHEDULED hasta que decida iniciarlo.
     */
    @PostMapping("/matches/{matchId}/force-deadline")
    public ResponseEntity<Match> forceDeadline(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchAdminService.forceDeadline(matchId));
    }

    /** Cerrar pronóstico + pasar a IN_PLAY en un solo paso. */
    @PostMapping("/matches/{matchId}/force-start")
    public ResponseEntity<Match> forceDeadlineAndStart(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchAdminService.forceDeadlineAndStart(matchId));
    }

    /** Cargar score parcial durante el partido (sin winner, sin FINISHED). */
    @PatchMapping("/matches/{matchId}/live-score")
    public ResponseEntity<MatchResultDto> updateLiveScore(
            @PathVariable Long matchId,
            @RequestBody MatchResultDto dto) {
        return ResponseEntity.ok(matchAdminService.updateLiveScore(matchId, dto));
    }
}
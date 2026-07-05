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

    /** Trae partidos en vivo de la API y los inserta en DB si no existen.
     *  NO cambia status ni guarda resultados — eso lo hace el admin manualmente. */
    @PostMapping("/sync/live")
    public ResponseEntity<Map<String, String>> syncLive() {
        tennisApiService.syncLiveEvents();
        return ResponseEntity.ok(Map.of("status", "Partidos en vivo sincronizados"));
    }

    /** Cambio manual de status (admin inicia/suspende/finaliza partido). */
    @PatchMapping("/matches/{matchId}/status")
    public ResponseEntity<Match> updateMatchStatus(
            @PathVariable Long matchId,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        return ResponseEntity.ok(matchAdminService.updateStatus(matchId, newStatus));
    }

    /** NUEVO — Forzar cierre de pronóstico + pasar a IN_PLAY en un solo paso. */
    @PostMapping("/matches/{matchId}/force-start")
    public ResponseEntity<Match> forceDeadlineAndStart(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchAdminService.forceDeadlineAndStart(matchId));
    }

    /** Solo cerrar pronóstico (sin cambiar status del partido). */
    @PostMapping("/matches/{matchId}/force-deadline")
    public ResponseEntity<Match> forceDeadlineOnly(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchAdminService.forceDeadlineOnly(matchId));
    }

    /** NUEVO — Cargar score parcial durante el partido (sin winner, sin FINISHED). */
    @PatchMapping("/matches/{matchId}/live-score")
    public ResponseEntity<MatchResultDto> updateLiveScore(
            @PathVariable Long matchId,
            @RequestBody MatchResultDto dto) {
        return ResponseEntity.ok(matchAdminService.updateLiveScore(matchId, dto));
    }

    /** Sincronizar partidos de mañana desde la API de tennis. */
    @PostMapping("/sync/tomorrow")
    public ResponseEntity<Map<String, String>> syncTomorrow() {
        tennisApiService.syncTomorrowEvents();
        return ResponseEntity.ok(Map.of("status", "Partidos de mañana sincronizados"));
    }
}

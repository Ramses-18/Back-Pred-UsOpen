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
        // saveResult ahora también marca status=FINISHED y actualEndTime internamente
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

    /** Sincroniza el schedule completo del día (a las 6 AM London se ejecuta solo,
     *  pero el admin puede forzarlo si se agregaron partidos a última hora).
    @PostMapping("/sync/schedule")
    public ResponseEntity<Map<String, String>> syncSchedule() {
        tennisApiService.syncDailySchedule();
        return ResponseEntity.ok(Map.of("status", "Schedule del día sincronizado"));
    }
    */

    /** Sincroniza status y scores en vivo de los partidos en juego. */
    @PostMapping("/sync/live")
    public ResponseEntity<Map<String, String>> syncLive() {
        tennisApiService.syncLiveStatus();
        return ResponseEntity.ok(Map.of("status", "Live sync ejecutado"));
    }

    /** Cambio manual de status (para casos que la API no refleja: lluvia, walkover
     *  declarado por el juez de silla antes que la API lo refleje, etc). */
    @PatchMapping("/matches/{matchId}/status")
    public ResponseEntity<Match> updateMatchStatus(
            @PathVariable Long matchId,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status"); // SCHEDULED | IN_PLAY | SUSPENDED | FINISHED | WALKOVER | RETIRED | ABANDONED
        return ResponseEntity.ok(matchAdminService.updateStatus(matchId, newStatus));
    }
}

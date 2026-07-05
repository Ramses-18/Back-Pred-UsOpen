package com.wimbledon.controller;

import com.wimbledon.dto.*;
import com.wimbledon.service.LeagueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leagues")
@RequiredArgsConstructor
@Slf4j
public class LeagueController {

    private final LeagueService leagueService;

    @PostMapping
    public ResponseEntity<LeagueDto> create(
            @Valid @RequestBody CreateLeagueRequest req,
            Authentication auth) {
        return ResponseEntity.ok(leagueService.createLeague(auth.getName(), req));
    }

    @PostMapping("/join")
    public ResponseEntity<LeagueDto> join(
            @Valid @RequestBody JoinLeagueRequest req,
            Authentication auth) {
        return ResponseEntity.ok(leagueService.joinLeague(auth.getName(), req));
    }

    @PostMapping("/{leagueId}/leave")
    public ResponseEntity<Map<String, String>> leave(
            @PathVariable Long leagueId,
            Authentication auth) {
        try {
            leagueService.leaveLeague(auth.getName(), leagueId);
            return ResponseEntity.ok(Map.of("status", "left"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{leagueId}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Long leagueId,
            Authentication auth) {
        try {
            leagueService.deleteLeague(auth.getName(), leagueId);
            return ResponseEntity.ok(Map.of("status", "deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeagueDto>> myLeagues(Authentication auth) {
        return ResponseEntity.ok(leagueService.getMyLeagues(auth.getName()));
    }

    @GetMapping("/{leagueId}/leaderboard")
    public ResponseEntity<LeagueLeaderboardDto> leaderboard(
            @PathVariable Long leagueId) {
        return ResponseEntity.ok(leagueService.getLeagueLeaderboard(leagueId));
    }
}
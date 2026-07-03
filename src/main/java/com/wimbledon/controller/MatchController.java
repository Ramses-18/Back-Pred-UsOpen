package com.wimbledon.controller;

import com.wimbledon.dto.*;
import com.wimbledon.service.PickService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Slf4j
public class MatchController {

    private final PickService pickService ;

    @GetMapping("/today")
    public ResponseEntity<List<MatchDto>> today(Authentication auth) {
        try {
            log.info("[/today] user={}", auth.getName());
            List<MatchDto> result = pickService.getTodayMatches(auth.getName());
            log.info("[/today] ✓ devolviendo {} partidos", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[/today] ✗ error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/{matchId}/pick")
    public ResponseEntity<PickDto> submitPick(
            @PathVariable Long matchId,
            @Valid @RequestBody PickRequest req,
            Authentication auth) {
        return ResponseEntity.ok(pickService.submitPick(matchId, req, auth.getName()));
    }
}

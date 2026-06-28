package com.wimbledon.controller;

import com.wimbledon.dto.*;
import com.wimbledon.service.PickService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final PickService pickService ;

    @GetMapping("/today")
    public ResponseEntity<List<MatchDto>> today(Authentication auth) {
        return ResponseEntity.ok(pickService.getTodayMatches(auth.getName()));
    }

    @PostMapping("/{matchId}/pick")
    public ResponseEntity<PickDto> submitPick(
            @PathVariable Long matchId,
            @Valid @RequestBody PickRequest req,
            Authentication auth) {
        return ResponseEntity.ok(pickService.submitPick(matchId, req, auth.getName()));
    }
}

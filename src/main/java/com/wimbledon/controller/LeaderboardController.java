package com.wimbledon.controller;

import com.wimbledon.dto.LeaderboardEntryDto;
import com.wimbledon.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final ScoreService scoreService ;

    @GetMapping
    public ResponseEntity<List<LeaderboardEntryDto>> get() {
        return ResponseEntity.ok(scoreService.buildLeaderboard());
    }
}

package com.wimbledon.controller;

import com.wimbledon.dto.TournamentDto;
import com.wimbledon.service.TournamentManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Slf4j
public class TournamentController {

    private final TournamentManagementService tmService;

    @GetMapping
    public ResponseEntity<List<TournamentDto>> all() {
        return ResponseEntity.ok(tmService.getAllTournaments());
    }

    @GetMapping("/active")
    public ResponseEntity<TournamentDto> active() {
        TournamentDto dto = tmService.getActiveTournament();
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<TournamentDto> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String slug = body.get("slug");
        if (name == null || slug == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(tmService.createTournament(name, slug));
    }
}
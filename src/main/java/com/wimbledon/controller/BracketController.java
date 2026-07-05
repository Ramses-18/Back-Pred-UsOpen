package com.wimbledon.controller;

import com.wimbledon.dto.BracketDto;
import com.wimbledon.dto.BracketMatchDto;
import com.wimbledon.service.BracketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bracket")
@RequiredArgsConstructor
public class BracketController {

    private final BracketService bracketService;

    /** GET /api/bracket — todo el bracket armado */
    @GetMapping
    public ResponseEntity<BracketDto> getBracket() {
        return ResponseEntity.ok(bracketService.getBracket());
    }

    /** POST /api/bracket/init — inicializa el bracket (solo una vez al inicio del torneo) */
    @PostMapping("/init")
    public ResponseEntity<String> initBracket() {
        bracketService.initBracket();
        return ResponseEntity.ok("Bracket inicializado");
    }

    /** PUT /api/bracket/{matchId} — actualiza un partido del bracket */
    @PutMapping("/{matchId}")
    public ResponseEntity<BracketMatchDto> updateMatch(
            @PathVariable Long matchId,
            @RequestBody BracketMatchDto dto) {
        return ResponseEntity.ok(bracketService.updateMatch(matchId, dto));
    }
}

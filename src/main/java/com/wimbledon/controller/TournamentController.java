package com.wimbledon.controller;

import com.wimbledon.dto.*;
import com.wimbledon.service.TournamentPickServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournament")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentPickServiceFacade facade;


    @GetMapping("/my-pick")
    public ResponseEntity<TournamentPickDto> myPick(Authentication auth) {
        return ResponseEntity.ok(facade.getMyPick(auth.getName()));
    }

    @PostMapping("/my-pick")
    public ResponseEntity<TournamentPickDto> savePick(
            @RequestBody TournamentPickRequest req, Authentication auth) {
        return ResponseEntity.ok(facade.savePick(req, auth.getName()));
    }

    @GetMapping("/result")
    public ResponseEntity<TournamentPickDto> getResult() {
        return ResponseEntity.ok(facade.getResult());
    }
}

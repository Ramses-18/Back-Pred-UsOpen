package com.wimbledon.controller;

import com.wimbledon.dto.TournamentPickDto;
import com.wimbledon.service.TournamentPickServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournament")
@RequiredArgsConstructor
public class TournamentPublicController {

    private final TournamentPickServiceFacade facade;

    @GetMapping("/result")
    public ResponseEntity<TournamentPickDto> getResult() {
        return ResponseEntity.ok(facade.getResult());
    }

    @GetMapping("/my-pick")
    public ResponseEntity<TournamentPickDto> getMyPick() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.ok(TournamentPickDto.builder()
                    .semis(java.util.List.of("", "", "", "")).build());
        }
        return ResponseEntity.ok(facade.getMyPick(auth.getName()));
    }
}
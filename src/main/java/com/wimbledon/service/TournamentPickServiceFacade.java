package com.wimbledon.service;

import com.wimbledon.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TournamentPickServiceFacade {
    private final TournamentService tournamentService;

    public TournamentPickDto getMyPick(String email)                      { return tournamentService.getMyPick(email); }
    public TournamentPickDto savePick(TournamentPickRequest req, String e) { return tournamentService.savePick(req, e); }
    public TournamentPickDto getResult()                                   { return tournamentService.getResult(); }
    public TournamentPickDto saveResult(TournamentPickRequest req)         { return tournamentService.saveResult(req); }
}

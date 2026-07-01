package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MatchAdminService {

    private final MatchRepository       matchRepo;
    private final MatchResultRepository resultRepo;

    public Match createMatch(MatchCreateRequest req) {
        return matchRepo.save(Match.builder()
            .matchDate(req.getMatchDate()).matchTime(req.getMatchTime())
            .court(req.getCourt()).player1(req.getPlayer1())
            .player2(req.getPlayer2()).round(req.getRound()).build());
    }

    @Transactional
    public MatchResultDto saveResult(Long matchId, MatchResultDto dto) {
        Match match = matchRepo.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado."));
        MatchResult res = resultRepo.findByMatchId(matchId)
            .orElse(MatchResult.builder().match(match).build());

        res.setWinner(dto.getWinner());
        res.setSetsWinner(dto.getSetsWinner());
        res.setSetsLoser(dto.getSetsLoser());
        res.setGameResult(dto.getGameResult());

        // Sets individuales
        res.setSet1W(dto.getSet1W()); res.setSet1L(dto.getSet1L());
        res.setSet2W(dto.getSet2W()); res.setSet2L(dto.getSet2L());
        res.setSet3W(dto.getSet3W()); res.setSet3L(dto.getSet3L());
        res.setSet4W(dto.getSet4W()); res.setSet4L(dto.getSet4L());
        res.setSet5W(dto.getSet5W()); res.setSet5L(dto.getSet5L());

        res.setEnteredAt(LocalDateTime.now());
        resultRepo.save(res);
        return dto;
    }

    public void deleteMatch(Long id) { matchRepo.deleteById(id); }
}

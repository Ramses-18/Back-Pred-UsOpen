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
        res.setSet1W(dto.getSet1W() != null ? dto.getSet1W() : 0); 
        res.setSet1L(dto.getSet1L() != null ? dto.getSet1L() : 0);
        res.setSet2W(dto.getSet2W() != null ? dto.getSet2W() : 0); 
        res.setSet2L(dto.getSet2L() != null ? dto.getSet2L() : 0);
        res.setSet3W(dto.getSet3W() != null ? dto.getSet3W() : 0); 
        res.setSet3L(dto.getSet3L() != null ? dto.getSet3L() : 0);
        res.setSet4W(dto.getSet4W() != null ? dto.getSet4W() : 0); 
        res.setSet4L(dto.getSet4L() != null ? dto.getSet4L() : 0);
        res.setSet5W(dto.getSet5W() != null ? dto.getSet5W() : 0); 
        res.setSet5L(dto.getSet5L() != null ? dto.getSet5L() : 0);

        res.setEnteredAt(LocalDateTime.now());
        resultRepo.save(res);
        return dto;
    }

/* 
    public MatchResultDto saveResult(Long matchId, MatchResultDto dto) {
        MatchResult result = MatchResult.builder()
            .matchId(matchId)
            .winner(dto.getWinner())
            .setsWinner(dto.getSetsWinner())
            .setsLoser(dto.getSetsLoser())
            .gameResult(dto.getGameResult())
            .enteredAt(LocalDateTime.now())
            // Protección contra nulos: Si el set es null, guardamos 0
            .set1W(dto.getSet1W() != null ? dto.getSet1W() : 0)
            .set1L(dto.getSet1L() != null ? dto.getSet1L() : 0)
            .set2W(dto.getSet2W() != null ? dto.getSet2W() : 0)
            .set2L(dto.getSet2L() != null ? dto.getSet2L() : 0)
            .set3W(dto.getSet3W() != null ? dto.getSet3W() : 0)
            .set3L(dto.getSet3L() != null ? dto.getSet3L() : 0)
            .set4W(dto.getSet4W() != null ? dto.getSet4W() : 0)
            .set4L(dto.getSet4L() != null ? dto.getSet4L() : 0)
            .set5W(dto.getSet5W() != null ? dto.getSet5W() : 0)
            .set5L(dto.getSet5L() != null ? dto.getSet5L() : 0)
            .build();
        
        resultRepo.save(result);
        return dto;
    }
*/
   





    public void deleteMatch(Long id) { matchRepo.deleteById(id); }
}

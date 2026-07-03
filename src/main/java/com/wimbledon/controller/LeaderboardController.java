package com.wimbledon.controller;

import com.wimbledon.dto.HistoricalPickDto;
import com.wimbledon.dto.LeaderboardEntryDto;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import com.wimbledon.service.ScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Slf4j
public class LeaderboardController {

    private final ScoreService      scoreService;
    private final UserRepository    userRepo;
    private final PickRepository    pickRepo;
    private final MatchResultRepository resultRepo;

    /** GET /api/leaderboard — tabla de posiciones completa */
    @GetMapping
    public ResponseEntity<List<LeaderboardEntryDto>> get() {
        return ResponseEntity.ok(scoreService.buildLeaderboard());
    }

    /**
     * GET /api/leaderboard/{userId}/picks
     * Devuelve todos los picks históricos de un usuario con su resultado real y puntos.
     * Ordenado por fecha de partido descendente (más reciente primero).
     */
    @GetMapping("/{userId}/picks")
    public ResponseEntity<List<HistoricalPickDto>> getUserPicks(@PathVariable Long userId) {
        log.info("[getUserPicks] userId={}", userId);

        User user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        List<Pick> picks = pickRepo.findByUserId(userId);
        log.info("[getUserPicks] encontrados {} picks para {}", picks.size(), user.getEmail());

        List<HistoricalPickDto> dtos = picks.stream()
            .map(p -> {
                Match m = p.getMatch();
                Optional<MatchResult> optRes = resultRepo.findByMatchId(m.getId());

                HistoricalPickDto.HistoricalPickDtoBuilder b = HistoricalPickDto.builder()
                    .matchId(m.getId())
                    .matchDate(m.getMatchDate())
                    .player1(m.getPlayer1())
                    .player2(m.getPlayer2())
                    .round(m.getRound())
                    .court(m.getCourt())
                    .matchStatus(m.getStatus())
                    .pickWinner(p.getWinner())
                    .pickSetsWinner(p.getSetsWinner())
                    .isCorrection(Boolean.TRUE.equals(p.getIsCorrection()));

                if (optRes.isPresent()) {
                    MatchResult r = optRes.get();
                    int pts = scoreService.calcPickPoints(p, r);
                    b.realWinner(r.getWinner())
                     .realSetsWinner(r.getSetsWinner())
                     .realScore(r.getGameResult())
                     .pointsEarned(pts);
                } else {
                    b.pointsEarned(0);
                }

                return b.build();
            })
            .sorted(Comparator.comparing(HistoricalPickDto::getMatchDate).reversed())
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}

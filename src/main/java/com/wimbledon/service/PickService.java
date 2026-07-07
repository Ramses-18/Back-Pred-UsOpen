package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PickService {

    private final MatchRepository          matchRepo;
    private final MatchResultRepository    resultRepo;
    private final PickRepository           pickRepo;
    private final DailyCorrectionRepository corrRepo;
    private final UserRepository           userRepo;
    private final ScoreService             scoreService;

    public List<MatchDto> getTodayMatches(String email) {
        User user = findUser(email);
        LocalDate today = LocalDate.now(ZoneId.of("America/Buenos_Aires"));
        List<Match> matches = matchRepo.findByMatchDateOrderByMatchTimeAsc(today);
        return matches.stream().map(m -> toDto(m, user)).collect(Collectors.toList());
    }

    public List<MatchDto> getTodayAndTomorrowMatches(String email) {
        User user = findUser(email);
        LocalDate today = LocalDate.now(ZoneId.of("America/Buenos_Aires"));
        LocalDate lookback = today.minusDays(5);
        List<Match> matches = matchRepo.findUpcomingOrActive(today, lookback);
        return matches.stream().map(m -> toDto(m, user)).collect(Collectors.toList());
    }

    private MatchDto toDto(Match m, User user) {
        Optional<MatchResult> optRes = resultRepo.findByMatchId(m.getId());
        Optional<Pick>        optPick = pickRepo.findByUserIdAndMatchId(user.getId(), m.getId());

        MatchResultDto resDto = optRes.map(r -> MatchResultDto.builder()
            .winner(r.getWinner())
            .setsWinner(r.getSetsWinner())
            .setsLoser(r.getSetsLoser())
            .gameResult(r.getGameResult())
            .set1W(r.getSet1W()).set1L(r.getSet1L())
            .set2W(r.getSet2W()).set2L(r.getSet2L())
            .set3W(r.getSet3W()).set3L(r.getSet3L())
            .set4W(r.getSet4W()).set4L(r.getSet4L())
            .set5W(r.getSet5W()).set5L(r.getSet5L())
            .build()
        ).orElse(null);

        PickDto pickDto = null;
        if (optPick.isPresent()) {
            Pick p = optPick.get();
            int pts = optRes.isPresent() ? scoreService.calcPickPoints(p, optRes.get()) : 0;
            pickDto = PickDto.builder()
            .matchId(m.getId())
            .winner(p.getWinner())
            .setsWinner(p.getSetsWinner())
            .isCorrection(Boolean.TRUE.equals(p.getIsCorrection()))
            .pointsEarned(pts)
            .set1W(safeInt(p.getSet1W())).set1L(safeInt(p.getSet1L()))
            .set2W(safeInt(p.getSet2W())).set2L(safeInt(p.getSet2L()))
            .set3W(safeInt(p.getSet3W())).set3L(safeInt(p.getSet3L()))
            .set4W(safeInt(p.getSet4W())).set4L(safeInt(p.getSet4L()))
            .set5W(safeInt(p.getSet5W())).set5L(safeInt(p.getSet5L()))
            .build();
        }

        return MatchDto.builder()
            .id(m.getId())
            .matchDate(m.getMatchDate())
            .matchTime(m.getMatchTime())
            .court(m.getCourt())
            .player1(m.getPlayer1())
            .player2(m.getPlayer2())
            .round(m.getRound())
            .orderInCourt(m.getOrderInCourt())
            .followsMatchId(m.getFollowsMatchId())
            .status(m.getStatus())
            .actualStartTime(m.getActualStartTime())
            .actualEndTime(m.getActualEndTime())
            .estimatedStartTime(computeEstimatedStart(m))
            .deadlineForced(Boolean.TRUE.equals(m.getDeadlineForced()))
            .result(resDto)
            .myPick(pickDto)
            .deadlinePassed(isDeadlinePassed(m))
            .build();
    }

    @Transactional
    public PickDto submitPick(Long matchId, PickRequest req, String email) {
        User  user  = findUser(email);
        Match match = matchRepo.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado."));

        boolean deadlinePassed = isDeadlinePassed(match);
        Optional<Pick> existing = pickRepo.findByUserIdAndMatchId(user.getId(), matchId);

        if (deadlinePassed) {
            if (!req.isUseCorrection())
                throw new IllegalStateException("El plazo de pronóstico ya cerró.");
            boolean alreadyUsed = corrRepo.existsByUserIdAndUsedDate(user.getId(), LocalDate.now());
            if (alreadyUsed)
                throw new IllegalStateException("Ya usaste tu corrección del día.");
            corrRepo.save(DailyCorrection.builder().user(user).usedDate(LocalDate.now()).build());
        }

        if (!req.getWinner().equalsIgnoreCase(match.getPlayer1())
         && !req.getWinner().equalsIgnoreCase(match.getPlayer2()))
            throw new IllegalArgumentException("El ganador debe ser uno de los dos jugadores.");

        Pick pick = existing.orElse(Pick.builder().user(user).match(match).build());
        pick.setWinner(req.getWinner());
        pick.setSetsWinner(req.getSetsWinner());
        pick.setSetsLoser(req.getSetsLoser());
        pick.setIsCorrection(req.isUseCorrection());
        pick.setUpdatedAt(LocalDateTime.now());

        pick.setSet1W(req.getSet1W());
        pick.setSet1L(req.getSet1L());
        pick.setSet2W(req.getSet2W());
        pick.setSet2L(req.getSet2L());
        pick.setSet3W(req.getSet3W());
        pick.setSet3L(req.getSet3L());
        pick.setSet4W(req.getSet4W());
        pick.setSet4L(req.getSet4L());
        pick.setSet5W(req.getSet5W());
        pick.setSet5L(req.getSet5L());

        pickRepo.save(pick);

        return PickDto.builder()
            .matchId(matchId)
            .winner(pick.getWinner())
            .setsWinner(pick.getSetsWinner())
            .isCorrection(pick.getIsCorrection())
            .set1W(safeInt(pick.getSet1W())).set1L(safeInt(pick.getSet1L()))
            .set2W(safeInt(pick.getSet2W())).set2L(safeInt(pick.getSet2L()))
            .set3W(safeInt(pick.getSet3W())).set3L(safeInt(pick.getSet3L()))
            .set4W(safeInt(pick.getSet4W())).set4L(safeInt(pick.getSet4L()))
            .set5W(safeInt(pick.getSet5W())).set5L(safeInt(pick.getSet5L()))
            .pointsEarned(0)
            .build();
    }

    /**
     * FIX Req 3: Deadline 100% manual por el admin.
     * Solo se cierra cuando el admin explicitamente fuerza el cierre (deadlineForced = true).
     * Ya NO se cierra automaticamente por horario ni por cambio de status.
     */
    private boolean isDeadlinePassed(Match match) {
        return Boolean.TRUE.equals(match.getDeadlineForced());
    }

    private LocalTime computeEstimatedStart(Match m) {
        if (m.getActualStartTime() != null) return m.getActualStartTime().toLocalTime();
        if (m.getMatchTime() != null) return m.getMatchTime();
        if (m.getFollowsMatchId() != null) {
            Match parent = matchRepo.findById(m.getFollowsMatchId()).orElse(null);
            if (parent != null && parent.getActualEndTime() != null) {
                return parent.getActualEndTime().plusMinutes(10).toLocalTime();
            }
            if (parent != null && parent.getMatchTime() != null) {
                return parent.getMatchTime().plusHours(2);
            }
        }
        return null;
    }

    private User findUser(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }

    private Integer safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
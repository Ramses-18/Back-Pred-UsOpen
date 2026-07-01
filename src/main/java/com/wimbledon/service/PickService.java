package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PickService {

    private final MatchRepository          matchRepo;
    private final MatchResultRepository    resultRepo;
    private final PickRepository           pickRepo;
    private final DailyCorrectionRepository corrRepo;
    private final UserRepository           userRepo;
    private final ScoreService             scoreService;

    public List<MatchDto> getTodayMatches(String email) {
        User user = findUser(email);
        List<Match> matches = matchRepo.findByMatchDateOrderByMatchTimeAsc(LocalDate.now());
        return matches.stream().map(m -> toDto(m, user)).collect(Collectors.toList());
    }

    private MatchDto toDto(Match m, User user) {
        Optional<MatchResult> optRes = resultRepo.findByMatchId(m.getId());
        Optional<Pick>        optPick = pickRepo.findByUserIdAndMatchId(user.getId(), m.getId());

        MatchResultDto resDto = optRes.map(r -> MatchResultDto.builder()
            .winner(r.getWinner())
            .setsWinner(r.getSetsWinner())
            .gamesWinner(r.getGamesWinner())
            .gamesLoser(r.getGamesLoser())
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
                .gamesWinner(p.getGamesWinner())
                .gamesLoser(p.getGamesLoser())
                .isCorrection(Boolean.TRUE.equals(p.getIsCorrection()))
                .pointsEarned(pts)
                .set1W(p.getSet1W()).set1L(p.getSet1L())
                .set2W(p.getSet2W()).set2L(p.getSet2L())
                .set3W(p.getSet3W()).set3L(p.getSet3L())
                .set4W(p.getSet4W()).set4L(p.getSet4L())
                .set5W(p.getSet5W()).set5L(p.getSet5L())
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

        if (existing.isPresent() && deadlinePassed) {
            if (!req.isUseCorrection())
                throw new IllegalStateException("El plazo de pronóstico ya cerró.");
            boolean alreadyUsed = corrRepo.existsByUserIdAndUsedDate(user.getId(), LocalDate.now());
            if (alreadyUsed)
                throw new IllegalStateException("Ya usaste tu corrección del día.");
            corrRepo.save(DailyCorrection.builder().user(user).usedDate(LocalDate.now()).build());
        }

        if (!existing.isPresent() && deadlinePassed && !req.isUseCorrection())
            throw new IllegalStateException("El plazo de pronóstico ya cerró.");

        if (!req.getWinner().equalsIgnoreCase(match.getPlayer1())
         && !req.getWinner().equalsIgnoreCase(match.getPlayer2()))
            throw new IllegalArgumentException("El ganador debe ser uno de los dos jugadores.");

        Pick pick = existing.orElse(Pick.builder().user(user).match(match).build());
        pick.setWinner(req.getWinner());
        pick.setSetsWinner(req.getSetsWinner());
        pick.setGamesWinner(req.getGamesWinner());
        pick.setGamesLoser(req.getGamesLoser());
        pick.setIsCorrection(req.isUseCorrection());
        pick.setUpdatedAt(LocalDateTime.now());

        // Guardar sets individuales
        pick.setSet1W(req.getSet1W()); pick.setSet1L(req.getSet1L());
        pick.setSet2W(req.getSet2W()); pick.setSet2L(req.getSet2L());
        pick.setSet3W(req.getSet3W()); pick.setSet3L(req.getSet3L());
        pick.setSet4W(req.getSet4W()); pick.setSet4L(req.getSet4L());
        pick.setSet5W(req.getSet5W()); pick.setSet5L(req.getSet5L());

        pickRepo.save(pick);

        return PickDto.builder()
            .matchId(matchId)
            .winner(pick.getWinner())
            .setsWinner(pick.getSetsWinner())
            .isCorrection(pick.getIsCorrection())
            .set1W(pick.getSet1W()).set1L(pick.getSet1L())
            .set2W(pick.getSet2W()).set2L(pick.getSet2L())
            .set3W(pick.getSet3W()).set3L(pick.getSet3L())
            .set4W(pick.getSet4W()).set4L(pick.getSet4L())
            .set5W(pick.getSet5W()).set5L(pick.getSet5L())
            .pointsEarned(0)
            .build();
    }

    // Cierre 5 minutos antes de cada partido individualmente
    private boolean isDeadlinePassed(Match match) {
        LocalDateTime matchStart = LocalDateTime.of(match.getMatchDate(), match.getMatchTime());
        LocalDateTime deadline = matchStart.minusMinutes(5);
        return LocalDateTime.now().isAfter(deadline);
    }

    private User findUser(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }
}

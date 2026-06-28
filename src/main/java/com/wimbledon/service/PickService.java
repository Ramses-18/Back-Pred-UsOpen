package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PickService {

    @Value("${app.deadline-offset-minutes:30}")
    private int deadlineOffsetMinutes;

    private final MatchRepository          matchRepo;
    private final MatchResultRepository    resultRepo;
    private final PickRepository           pickRepo;
    private final DailyCorrectionRepository corrRepo;
    private final UserRepository           userRepo;
    private final ScoreService             scoreService;


    /** Returns today's matches enriched with pick + result for the caller */
    public List<MatchDto> getTodayMatches(String email) {
        User user = findUser(email);
        List<Match> matches = matchRepo.findByMatchDateOrderByMatchTimeAsc(LocalDate.now());
        return matches.stream().map(m -> toDto(m, user)).collect(Collectors.toList());
    }

    private MatchDto toDto(Match m, User user) {
        Optional<MatchResult> optRes = resultRepo.findByMatchId(m.getId());
        Optional<Pick>        optPick = pickRepo.findByUserIdAndMatchId(user.getId(), m.getId());

        MatchResultDto resDto = optRes.map(r -> MatchResultDto.builder()
            .winner(r.getWinner()).setsWinner(r.getSetsWinner())
            .gamesWinner(r.getGamesWinner()).gamesLoser(r.getGamesLoser()).build()
        ).orElse(null);

        PickDto pickDto = null;
        if (optPick.isPresent()) {
            Pick p = optPick.get();
            int pts = 0;
            if (resDto != null) {
                if (p.getWinner().equalsIgnoreCase(resDto.getWinner())) {
                    pts += 1;
                    if (Objects.equals(p.getSetsWinner(), resDto.getSetsWinner())) pts += 3;
                    if (Objects.equals(p.getSetsWinner(), resDto.getSetsWinner())
                     && Objects.equals(p.getGamesWinner(), resDto.getGamesWinner())
                     && Objects.equals(p.getGamesLoser(),  resDto.getGamesLoser()))  pts += 10;
                }
            }
            pickDto = PickDto.builder()
                .matchId(m.getId()).winner(p.getWinner())
                .setsWinner(p.getSetsWinner()).gamesWinner(p.getGamesWinner()).gamesLoser(p.getGamesLoser())
                .isCorrection(Boolean.TRUE.equals(p.getIsCorrection())).pointsEarned(pts).build();
        }

        return MatchDto.builder()
            .id(m.getId()).matchDate(m.getMatchDate()).matchTime(m.getMatchTime())
            .court(m.getCourt()).player1(m.getPlayer1()).player2(m.getPlayer2()).round(m.getRound())
            .result(resDto).myPick(pickDto).deadlinePassed(isDeadlinePassed(m))
            .build();
    }

    /** Submit or correct a pick */
    @Transactional
    public PickDto submitPick(Long matchId, PickRequest req, String email) {
        User  user  = findUser(email);
        Match match = matchRepo.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado."));

        boolean deadlinePassed = isDeadlinePassed(match);
        Optional<Pick> existing = pickRepo.findByUserIdAndMatchId(user.getId(), matchId);

        if (existing.isPresent() && deadlinePassed) {
            // Only allowed if using correction
            if (!req.isUseCorrection()) throw new IllegalStateException("El plazo de pronóstico ya cerró.");
            boolean alreadyUsed = corrRepo.existsByUserIdAndUsedDate(user.getId(), LocalDate.now());
            if (alreadyUsed) throw new IllegalStateException("Ya usaste tu corrección del día.");
            corrRepo.save(DailyCorrection.builder().user(user).usedDate(LocalDate.now()).build());
        }

        if (!existing.isPresent() && deadlinePassed && !req.isUseCorrection())
            throw new IllegalStateException("El plazo de pronóstico ya cerró.");

        // Validate winner belongs to match
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
        pickRepo.save(pick);

        return PickDto.builder()
            .matchId(matchId).winner(pick.getWinner())
            .setsWinner(pick.getSetsWinner()).gamesWinner(pick.getGamesWinner())
            .gamesLoser(pick.getGamesLoser()).isCorrection(pick.getIsCorrection())
            .pointsEarned(0).build();
    }

    private boolean isDeadlinePassed(Match match) {
        LocalDateTime firstMatchDt = LocalDateTime.of(match.getMatchDate(), match.getMatchTime());
        LocalDateTime deadline = firstMatchDt.minusMinutes(deadlineOffsetMinutes);
        return LocalDateTime.now().isAfter(deadline);
    }

    private User findUser(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }
}

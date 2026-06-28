package com.wimbledon.service;

import com.wimbledon.dto.LeaderboardEntryDto;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private static final int PTS_WINNER        = 1;
    private static final int PTS_SETS          = 3;
    private static final int PTS_EXACT         = 10;
    private static final int PTS_CHAMPION      = 15;
    private static final int PTS_SEMIFINALIST  = 10;

    private final UserRepository          userRepo;
    private final PickRepository          pickRepo;
    private final MatchResultRepository   resultRepo;
    private final DailyCorrectionRepository corrRepo;
    private final TournamentPickRepository tPickRepo;
    private final TournamentResultRepository tResultRepo;

    /** Calculates daily match points for a single user */
    public int calcDailyPoints(User user) {
        int pts = 0;
        List<Pick> picks = pickRepo.findTodayPicksByUserId(user.getId());
        for (Pick pick : picks) {
            Optional<MatchResult> optRes = resultRepo.findByMatchId(pick.getMatch().getId());
            if (optRes.isEmpty()) continue;
            MatchResult res = optRes.get();

            if (pick.getWinner().equalsIgnoreCase(res.getWinner())) {
                pts += PTS_WINNER;
                if (pick.getSetsWinner() != null && pick.getSetsWinner().equals(res.getSetsWinner()))
                    pts += PTS_SETS;
                if (pick.getSetsWinner() != null && pick.getSetsWinner().equals(res.getSetsWinner())
                        && pick.getGamesWinner() != null && pick.getGamesWinner().equals(res.getGamesWinner())
                        && pick.getGamesLoser()  != null && pick.getGamesLoser().equals(res.getGamesLoser()))
                    pts += PTS_EXACT;
            }
        }
        return pts;
    }

    /** Calculates tournament bonus points for a single user */
    public int calcTournamentPoints(User user) {
        Optional<TournamentResult> optTr = tResultRepo.findTopByOrderByIdDesc();
        if (optTr.isEmpty()) return 0;
        TournamentResult tr = optTr.get();

        Optional<TournamentPick> optTp = tPickRepo.findByUserId(user.getId());
        if (optTp.isEmpty()) return 0;
        TournamentPick tp = optTp.get();

        int pts = 0;
        if (tr.getChampion() != null && tr.getChampion().equalsIgnoreCase(tp.getChampion()))
            pts += PTS_CHAMPION;

        Set<String> realSemis = Set.of(
            orEmpty(tr.getSemi1()), orEmpty(tr.getSemi2()),
            orEmpty(tr.getSemi3()), orEmpty(tr.getSemi4())
        );
        for (String s : List.of(orEmpty(tp.getSemi1()), orEmpty(tp.getSemi2()),
                                 orEmpty(tp.getSemi3()), orEmpty(tp.getSemi4()))) {
            if (!s.isEmpty() && realSemis.contains(s)) pts += PTS_SEMIFINALIST;
        }
        return pts;
    }

    /** Builds full leaderboard sorted by total points desc */
    public List<LeaderboardEntryDto> buildLeaderboard() {
        List<User> users = userRepo.findAll();
        LocalDate today = LocalDate.now();

        List<LeaderboardEntryDto> entries = users.stream().map(u -> {
            int daily      = calcDailyPoints(u);
            int tournament = calcTournamentPoints(u);
            boolean corrUsed = corrRepo.existsByUserIdAndUsedDate(u.getId(), today);
            return LeaderboardEntryDto.builder()
                .name(u.getName())
                .email(u.getEmail())
                .totalPoints(daily + tournament)
                .dailyPoints(daily)
                .tournamentPoints(tournament)
                .correctionUsedToday(corrUsed)
                .build();
        }).collect(Collectors.toList());

        entries.sort(Comparator.comparingInt(LeaderboardEntryDto::getTotalPoints).reversed());
        for (int i = 0; i < entries.size(); i++) entries.get(i).setRank(i + 1);
        return entries;
    }

    private String orEmpty(String s) { return s == null ? "" : s.toLowerCase().trim(); }
}

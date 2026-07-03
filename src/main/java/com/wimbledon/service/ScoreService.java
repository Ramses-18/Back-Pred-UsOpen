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

    private static final int PTS_WINNER       = 1;
    private static final int PTS_SETS         = 3;
    private static final int PTS_EXACT        = 10;
    private static final int PTS_CHAMPION     = 15;
    private static final int PTS_SEMIFINALIST = 10;

    private final UserRepository             userRepo;
    private final PickRepository             pickRepo;
    private final MatchResultRepository      resultRepo;
    private final DailyCorrectionRepository  corrRepo;
    private final TournamentPickRepository   tPickRepo;
    private final TournamentResultRepository tResultRepo;

    /** Calcula puntos de un pick individual contra su resultado */
    public int calcPickPoints(Pick pick, MatchResult res) {
        int pts = 0;
        if (!pick.getWinner().equalsIgnoreCase(res.getWinner())) return 0;

        pts += PTS_WINNER;

        // +3 si acertó resultado en sets (setsWinner y setsLoser)
        // Calcular setsWinner y setsLoser reales contando los sets individuales
        // (más confiable que lo que el admin haya cargado a mano)
        int[] realCounts = contarSets(res);
        Integer realSetsWinner = realCounts[0] > 0 ? realCounts[0] : res.getSetsWinner();
        Integer realSetsLoser  = realCounts[1] > 0 ? realCounts[1] : res.getSetsLoser();

        // Si el pick no tiene setsWinner/setsLoser explícitos, contarlos de sus sets individuales
        int[] pickCounts = contarSetsPick(pick);
        Integer pickSetsWinner = pickCounts[0] > 0 ? pickCounts[0] : pick.getSetsWinner();
        Integer pickSetsLoser  = pickCounts[1] > 0 ? pickCounts[1] : pick.getSetsLoser();

        boolean setsWinnerOk = pickSetsWinner != null && realSetsWinner != null
            && pickSetsWinner.equals(realSetsWinner);

        boolean setsLoserOk = pickSetsLoser != null && realSetsLoser != null
            && pickSetsLoser.equals(realSetsLoser);

        if (setsWinnerOk && setsLoserOk) {
            pts += PTS_SETS;
        }

        // +10 si acertó el resultado exacto set a set
        if (esResultadoExacto(pick, res))
            pts += PTS_EXACT;

        return pts;
    }

    /**
     * Cuenta sets ganados por winner y loser a partir de los sets individuales del resultado.
     * Devuelve [setsWinner, setsLoser].
     * Solo cuenta sets que tengan valores > 0 en ambos lados (un set jugado).
     */
    private int[] contarSets(MatchResult res) {
        int w = 0, l = 0;
        Integer[][] sets = {
            {res.getSet1W(), res.getSet1L()},
            {res.getSet2W(), res.getSet2L()},
            {res.getSet3W(), res.getSet3L()},
            {res.getSet4W(), res.getSet4L()},
            {res.getSet5W(), res.getSet5L()},
        };
        for (Integer[] s : sets) {
            if (s[0] != null && s[1] != null && s[0] > 0 && s[1] >= 0) {
                if (s[0] > s[1]) w++;
                else if (s[1] > s[0]) l++;
            }
        }
        return new int[]{w, l};
    }

    /** Igual que contarSets pero para un Pick */
    private int[] contarSetsPick(Pick pick) {
        int w = 0, l = 0;
        Integer[][] sets = {
            {pick.getSet1W(), pick.getSet1L()},
            {pick.getSet2W(), pick.getSet2L()},
            {pick.getSet3W(), pick.getSet3L()},
            {pick.getSet4W(), pick.getSet4L()},
            {pick.getSet5W(), pick.getSet5L()},
        };
        for (Integer[] s : sets) {
            if (s[0] != null && s[1] != null && s[0] > 0 && s[1] >= 0) {
                if (s[0] > s[1]) w++;
                else if (s[1] > s[0]) l++;
            }
        }
        return new int[]{w, l};
    }

    /** Compara los sets individuales del pick con el resultado real */
    private boolean esResultadoExacto(Pick pick, MatchResult res) {
        // Necesita al menos el set 1
        if (pick.getSet1W() == null || res.getSet1W() == null) return false;

        boolean s1 = eq(pick.getSet1W(), res.getSet1W()) && eq(pick.getSet1L(), res.getSet1L());
        boolean s2 = eq(pick.getSet2W(), res.getSet2W()) && eq(pick.getSet2L(), res.getSet2L());

        // Set 3-5: si ambos son null se considera correcto (partido no llegó a ese set)
        boolean s3 = ambosNull(pick.getSet3W(), res.getSet3W())
            || (eq(pick.getSet3W(), res.getSet3W()) && eq(pick.getSet3L(), res.getSet3L()));
        boolean s4 = ambosNull(pick.getSet4W(), res.getSet4W())
            || (eq(pick.getSet4W(), res.getSet4W()) && eq(pick.getSet4L(), res.getSet4L()));
        boolean s5 = ambosNull(pick.getSet5W(), res.getSet5W())
            || (eq(pick.getSet5W(), res.getSet5W()) && eq(pick.getSet5L(), res.getSet5L()));

        return s1 && s2 && s3 && s4 && s5;
    }

    private boolean eq(Integer a, Integer b) { return a != null && a.equals(b); }
    private boolean ambosNull(Integer a, Integer b) { return a == null && b == null; }

    /** Puntos diarios totales de un usuario */
    public int calcDailyPoints(User user) {
        int pts = 0;
        List<Pick> picks = pickRepo.findByUserId(user.getId());
        for (Pick pick : picks) {
            Optional<MatchResult> optRes = resultRepo.findByMatchId(pick.getMatch().getId());
            if (optRes.isEmpty()) continue;
            pts += calcPickPoints(pick, optRes.get());
        }
        return pts;
    }

    /** Puntos de torneo de un usuario */
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

        Set<String> realSemis = new HashSet<>(Arrays.asList(
            orEmpty(tr.getSemi1()), orEmpty(tr.getSemi2()),
            orEmpty(tr.getSemi3()), orEmpty(tr.getSemi4())
        ));
        for (String s : List.of(orEmpty(tp.getSemi1()), orEmpty(tp.getSemi2()),
                                 orEmpty(tp.getSemi3()), orEmpty(tp.getSemi4()))) {
            if (!s.isEmpty() && realSemis.contains(s)) pts += PTS_SEMIFINALIST;
        }
        return pts;
    }

    /** Tabla de posiciones completa */
    public List<LeaderboardEntryDto> buildLeaderboard() {
        List<User> users = userRepo.findAll();
        LocalDate today = LocalDate.now();

        List<LeaderboardEntryDto> entries = users.stream().map(u -> {
            int daily      = calcDailyPoints(u);
            int tournament = calcTournamentPoints(u);
            boolean corrUsed = corrRepo.existsByUserIdAndUsedDate(u.getId(), today);
            return LeaderboardEntryDto.builder()
                .userId(u.getId())
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

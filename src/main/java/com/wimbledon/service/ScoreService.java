package com.wimbledon.service;

import com.wimbledon.dto.LeaderboardEntryDto;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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

    /**
     * Calcula puntos de un pick individual contra su resultado.
     * FIX BUG 4: normaliza nombres (trim + lowercase) antes de comparar.
     */
    public int calcPickPoints(Pick pick, MatchResult res) {
        if (pick == null || res == null) return 0;

        String pickWinner = normalize(pick.getWinner());
        String resWinner  = normalize(res.getWinner());
        if (pickWinner.isEmpty() || resWinner.isEmpty()) return 0;
        if (!pickWinner.equals(resWinner)) return 0;

        int pts = PTS_WINNER;  // +1

        // +3 si acertó los sets ganados
        if (pick.getSetsWinner() != null && res.getSetsWinner() != null
            && pick.getSetsWinner().equals(res.getSetsWinner())) {
            pts += PTS_SETS;
        }

        // +10 si acertó el resultado exacto set a set
        if (esResultadoExacto(pick, res)) {
            pts += PTS_EXACT;
        }

        log.debug("[calcPickPoints] pick.matchId={} winner={} pts={}",
            pick.getMatch() != null ? pick.getMatch().getId() : null, pick.getWinner(), pts);
        return pts;
    }

    /**
     * FIX BUG 2 + 3: trata 0 (no cargado) igual que null (no jugado).
     * Antes: si el admin no cargaba sets 3-5, se guardaban como 0 y
     * ambosNull(null, 0) era false, así que nunca se consideraba "exacto".
     * Ahora: cualquier set que sea null O 0 se considera "no jugado" y
     * solo se exige que el pick también lo tenga vacío (null).
     */
    private boolean esResultadoExacto(Pick pick, MatchResult res) {
        // Necesita al menos el set 1 con valor real
        if (!esSetJugado(pick.getSet1W()) || !esSetJugado(res.getSet1W())) return false;

        boolean s1 = eqSet(pick.getSet1W(), res.getSet1W())
                  && eqSet(pick.getSet1L(), res.getSet1L());
        boolean s2 = ambosNoJugados(pick.getSet2W(), res.getSet2W())
                  || (eqSet(pick.getSet2W(), res.getSet2W())
                   && eqSet(pick.getSet2L(), res.getSet2L()));
        boolean s3 = ambosNoJugados(pick.getSet3W(), res.getSet3W())
                  || (eqSet(pick.getSet3W(), res.getSet3W())
                   && eqSet(pick.getSet3L(), res.getSet3L()));
        boolean s4 = ambosNoJugados(pick.getSet4W(), res.getSet4W())
                  || (eqSet(pick.getSet4W(), res.getSet4W())
                   && eqSet(pick.getSet4L(), res.getSet4L()));
        boolean s5 = ambosNoJugados(pick.getSet5W(), res.getSet5W())
                  || (eqSet(pick.getSet5W(), res.getSet5W())
                   && eqSet(pick.getSet5L(), res.getSet5L()));

        return s1 && s2 && s3 && s4 && s5;
    }

    /** Un set se considera "jugado" si no es null y no es 0 (o sea, tiene games reales). */
    private boolean esSetJugado(Integer i) { return i != null && i > 0; }

    /** Compara dos valores de set: solo da true si ambos son jugados y son iguales. */
    private boolean eqSet(Integer a, Integer b) {
        if (!esSetJugado(a) || !esSetJugado(b)) return false;
        return a.equals(b);
    }

    /** True si AMBOS son "no jugados" (null o 0). Esto hace que un set no jugado
     *  en el resultado se considere correcto si el pick tampoco lo cargó. */
    private boolean ambosNoJugados(Integer a, Integer b) {
        return !esSetJugado(a) && !esSetJugado(b);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    /**
     * FIX BUG 1: ahora suma TODOS los picks del usuario (no solo los de hoy).
     * La tabla de posiciones es acumulada por todo el torneo, no diaria.
     */
    public int calcDailyPoints(User user) {
        int pts = 0;
        List<Pick> picks = pickRepo.findByUserId(user.getId());
        log.debug("[calcDailyPoints] user={} picks totales={}", user.getEmail(), picks.size());
        for (Pick pick : picks) {
            Optional<MatchResult> optRes = resultRepo.findByMatchId(pick.getMatch().getId());
            if (optRes.isEmpty()) continue;
            pts += calcPickPoints(pick, optRes.get());
        }
        log.debug("[calcDailyPoints] user={} pts acumulados={}", user.getEmail(), pts);
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

        log.info("[buildLeaderboard] calculando para {} usuarios", users.size());

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

        log.info("[buildLeaderboard] top 3: {}",
            entries.stream().limit(3)
                .map(e -> e.getName() + "=" + e.getTotalPoints())
                .collect(Collectors.joining(", ")));

        return entries;
    }

    private String orEmpty(String s) { return s == null ? "" : s.toLowerCase().trim(); }
}
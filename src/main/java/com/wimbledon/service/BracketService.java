package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.BracketMatch;
import com.wimbledon.entity.Match;
import com.wimbledon.entity.MatchResult;
import com.wimbledon.repository.BracketMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BracketService {

    private final BracketMatchRepository bracketRepo;

    // Definición de rondas del US Open (128 jugadores)
    // key, label, cantidad de partidos
    private static final String[][] ROUNDS_DEF = {
        {"R128", "1ra ronda",       "64"},
        {"R64",  "2da ronda",       "32"},
        {"R32",  "3ra ronda",       "16"},
        {"R16",  "4ta ronda",        "8"},
        {"QF",   "Cuartos de final", "4"},
        {"SF",   "Semifinales",      "2"},
        {"F",    "Final",            "1"},
    };

    /** Devuelve todo el bracket armado */
    public BracketDto getBracket() {
        List<BracketMatch> all = bracketRepo.findAllByOrderByRoundAscPositionInRoundAsc();

        List<BracketMatchDto> dtos = all.stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        List<BracketDto.RoundInfo> rounds = Arrays.stream(ROUNDS_DEF)
            .map(r -> BracketDto.RoundInfo.builder()
                .key(r[0]).label(r[1]).count(Integer.parseInt(r[2]))
                .build())
            .collect(Collectors.toList());

        // Buscar el campeón (winner de la final)
        String champion = all.stream()
            .filter(m -> "F".equals(m.getRound()) && m.getWinner() != null)
            .map(BracketMatch::getWinner)
            .findFirst()
            .orElse(null);

        return BracketDto.builder()
            .matches(dtos)
            .champion(champion)
            .rounds(rounds)
            .build();
    }

    /**
     * Inicializa el bracket completo con partidos vacíos.
     * US Open: 64 + 32 + 16 + 8 + 4 + 2 + 1 = 127 partidos.
     * Solo se ejecuta una vez al inicio del torneo.
     */
    @Transactional
    public void initBracket() {
        if (bracketRepo.count() > 0) {
            throw new IllegalStateException("El bracket ya está inicializado. Eliminar todos los BracketMatch para reiniciar.");
        }

        log.info("[initBracket] Creando estructura del bracket US Open (127 partidos)...");

        // Crear todas las rondas con partidos vacíos
        for (String[] roundDef : ROUNDS_DEF) {
            String round = roundDef[0];
            int count = Integer.parseInt(roundDef[2]);
            for (int i = 1; i <= count; i++) {
                BracketMatch m = BracketMatch.builder()
                    .round(round)
                    .positionInRound(i)
                    .player1(null)
                    .player2(null)
                    .winner(null)
                    .status("SCHEDULED")
                    .build();
                bracketRepo.save(m);
            }
        }

        // Llenar sourceMatch1 y sourceMatch2: cada partido viene de 2 partidos de la ronda anterior
        for (int r = 1; r < ROUNDS_DEF.length; r++) {
            String currentRound = ROUNDS_DEF[r][0];
            String previousRound = ROUNDS_DEF[r-1][0];
            int currentCount = Integer.parseInt(ROUNDS_DEF[r][2]);

            for (int i = 1; i <= currentCount; i++) {
                final String roundForError = currentRound;
                final int posForError = i;
                BracketMatch current = bracketRepo.findByRoundAndPositionInRound(currentRound, i)
                    .orElseThrow(() -> new IllegalStateException("No se encontró partido " + roundForError + " #" + posForError));

                // sourceMatch1 = partido previousRound #(2*i - 1)
                // sourceMatch2 = partido previousRound #(2*i)
                Long src1 = bracketRepo.findByRoundAndPositionInRound(previousRound, 2*i - 1)
                    .map(BracketMatch::getId).orElse(null);
                Long src2 = bracketRepo.findByRoundAndPositionInRound(previousRound, 2*i)
                    .map(BracketMatch::getId).orElse(null);

                current.setSourceMatch1(src1);
                current.setSourceMatch2(src2);
                bracketRepo.save(current);
            }
        }

        log.info("[initBracket] ✓ Bracket creado con {} partidos", bracketRepo.count());
    }

    /**
     * Actualiza un partido del bracket (admin carga jugadores y/o resultado).
     * Si se setea winner, automáticamente propaga a los source matches del partido siguiente.
     */
    @Transactional
    public BracketMatchDto updateMatch(Long matchId, BracketMatchDto dto) {
        log.info("[updateMatch] matchId={}, player1={}, player2={}, winner={}",
            matchId, dto.getPlayer1(), dto.getPlayer2(), dto.getWinner());

        BracketMatch m = bracketRepo.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido del bracket no encontrado."));

        // Actualizar jugadores (siempre, null limpia)
        m.setPlayer1(dto.getPlayer1() != null ? (dto.getPlayer1().trim().isEmpty() ? null : dto.getPlayer1().trim()) : m.getPlayer1());
        m.setPlayer2(dto.getPlayer2() != null ? (dto.getPlayer2().trim().isEmpty() ? null : dto.getPlayer2().trim()) : m.getPlayer2());

        // Actualizar resultado: si viene winner no-null, usarlo; si viene status=SCHEDULED, limpiar
        if (dto.getStatus() != null && "SCHEDULED".equals(dto.getStatus())) {
            m.setWinner(null);
            m.setScoreStr(null);
            m.setSetsWinner(null);
            m.setSetsLoser(null);
            m.setStatus("SCHEDULED");
        } else if (dto.getWinner() != null) {
            String winner = dto.getWinner().trim().isEmpty() ? null : dto.getWinner().trim();
            m.setWinner(winner);
            m.setStatus(winner == null ? "SCHEDULED" : "FINISHED");
        }

        if (dto.getScoreStr() != null && m.getWinner() != null) m.setScoreStr(dto.getScoreStr());
        if (dto.getSetsWinner() != null && m.getWinner() != null) m.setSetsWinner(dto.getSetsWinner());
        if (dto.getSetsLoser() != null && m.getWinner() != null) m.setSetsLoser(dto.getSetsLoser());

        m.setUpdatedAt(LocalDateTime.now());
        bracketRepo.save(m);

        // Si se seteó un winner, propagar al partido siguiente que tenga este como source
        if (m.getWinner() != null) {
            propagateWinner(m);
        }

        log.info("[updateMatch] ✓ actualizado: {} vs {} → ganador {}",
            m.getPlayer1(), m.getPlayer2(), m.getWinner());
        return toDto(m);
    }

    /**
     * Sincroniza un resultado de partido (matches table) al bracket.
     * Se llama cuando el admin guarda un resultado final.
     * Busca el bracket_match de la misma ronda que contenga ambos jugadores y lo actualiza.
     */
    @Transactional
    public void syncMatchResultToBracket(Match match, MatchResult result) {
        String round = match.getRound();
        if (round == null || !isBracketRound(round)) {
            log.info("[syncMatchResult] Partido {} no es de ronda de bracket (round={}), se ignora.", match.getId(), round);
            return;
        }

        String p1 = match.getPlayer1();
        String p2 = match.getPlayer2();

        // Buscar bracket_match de la misma ronda que tenga player1 y player2 coincidentes
        Optional<BracketMatch> optBm = bracketRepo.findByRound(round).stream()
            .filter(bm -> {
                if (bm.getPlayer1() == null || bm.getPlayer2() == null) return false;
                return (bm.getPlayer1().equalsIgnoreCase(p1) && bm.getPlayer2().equalsIgnoreCase(p2))
                    || (bm.getPlayer1().equalsIgnoreCase(p2) && bm.getPlayer2().equalsIgnoreCase(p1));
            })
            .findFirst();

        if (optBm.isPresent()) {
            BracketMatch bm = optBm.get();
            bm.setPlayer1(p1);
            bm.setPlayer2(p2);
            bm.setWinner(result.getWinner());
            bm.setScoreStr(result.getGameResult());
            bm.setSetsWinner(result.getSetsWinner());
            bm.setSetsLoser(result.getSetsLoser());
            bm.setStatus("FINISHED");
            bm.setUpdatedAt(LocalDateTime.now());
            bracketRepo.save(bm);

            log.info("[syncMatchResult] ✓ Bracket actualizado: {} vs {} → {} (ronda {})",
                p1, p2, result.getWinner(), round);

            // Propagar ganador
            if (result.getWinner() != null) {
                propagateWinner(bm);
            }
        } else {
            log.info("[syncMatchResult] No se encontró bracket_match para {} vs {} en ronda {}", p1, p2, round);
        }
    }

    /**
     * Cuando se crea un partido de ronda de bracket, lo linka al bracket_match.
     * Si bracketPosition != null, usa esa posición específica.
     * Si no, busca el primer slot vacío de esa ronda.
     */
    @Transactional
    public void linkNewMatchToBracket(Match match, Integer bracketPosition) {
        String round = match.getRound();
        if (round == null || !isBracketRound(round)) {
            return;
        }

        String p1 = match.getPlayer1();
        String p2 = match.getPlayer2();

        Optional<BracketMatch> optBm;

        if (bracketPosition != null) {
            // El admin eligió una posición específica
            optBm = bracketRepo.findByRoundAndPositionInRound(round, bracketPosition);
        } else {
            // Auto: primer slot vacío de esa ronda
            optBm = bracketRepo.findByRound(round).stream()
                .filter(bm -> bm.getPlayer1() == null || bm.getPlayer2() == null)
                .filter(bm -> {
                    if (bm.getPlayer1() != null && (bm.getPlayer1().equalsIgnoreCase(p1) || bm.getPlayer1().equalsIgnoreCase(p2))) return false;
                    if (bm.getPlayer2() != null && (bm.getPlayer2().equalsIgnoreCase(p1) || bm.getPlayer2().equalsIgnoreCase(p2))) return false;
                    return true;
                })
                .findFirst();
        }

        if (optBm.isPresent()) {
            BracketMatch bm = optBm.get();
            bm.setPlayer1(p1);
            bm.setPlayer2(p2);
            bm.setMatchId(match.getId());
            bracketRepo.save(bm);
            log.info("[linkNewMatch] ✓ {} vs {} asignados al bracket {} #{}", p1, p2, round, bm.getPositionInRound());
        } else {
            log.info("[linkNewMatch] No hay slot disponible en bracket para {} vs {} en ronda {} pos={}", p1, p2, round, bracketPosition);
        }
    }

    private boolean isBracketRound(String round) {
        return Arrays.stream(ROUNDS_DEF).anyMatch(r -> r[0].equals(round));
    }

    /**
     * Cuando un partido termina, propagar el ganador al partido siguiente que lo tenga como source.
     * Buscar partidos donde sourceMatch1 == m.id o sourceMatch2 == m.id.
     */
    private void propagateWinner(BracketMatch finished) {
        List<BracketMatch> all = bracketRepo.findAllByOrderByRoundAscPositionInRoundAsc();
        for (BracketMatch next : all) {
            if (finished.getId().equals(next.getSourceMatch1())) {
                next.setPlayer1(finished.getWinner());
                next.setUpdatedAt(LocalDateTime.now());
                bracketRepo.save(next);
                log.info("[propagateWinner] ✓ partido {} ahora tiene player1={}", next.getId(), finished.getWinner());
            }
            if (finished.getId().equals(next.getSourceMatch2())) {
                next.setPlayer2(finished.getWinner());
                next.setUpdatedAt(LocalDateTime.now());
                bracketRepo.save(next);
                log.info("[propagateWinner] ✓ partido {} ahora tiene player2={}", next.getId(), finished.getWinner());
            }
        }
    }

    private BracketMatchDto toDto(BracketMatch m) {
        return BracketMatchDto.builder()
            .id(m.getId())
            .round(m.getRound())
            .positionInRound(m.getPositionInRound())
            .player1(m.getPlayer1())
            .player2(m.getPlayer2())
            .winner(m.getWinner())
            .scoreStr(m.getScoreStr())
            .setsWinner(m.getSetsWinner())
            .setsLoser(m.getSetsLoser())
            .sourceMatch1(m.getSourceMatch1())
            .sourceMatch2(m.getSourceMatch2())
            .matchId(m.getMatchId())
            .status(m.getStatus())
            .build();
    }
}
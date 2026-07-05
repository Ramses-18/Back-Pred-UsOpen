package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchAdminService {

    private final MatchRepository       matchRepo;
    private final MatchResultRepository resultRepo;
    private final CourtQueueService     courtQueueService;

    public Match createMatch(MatchCreateRequest req) {
        Integer maxOrder = matchRepo.findMaxOrderInCourt(req.getMatchDate(), req.getCourt());
        int orderInCourt = (maxOrder == null ? 0 : maxOrder) + 1;

        if (req.getFollowsMatchId() != null) {
            Match parent = matchRepo.findById(req.getFollowsMatchId())
                .orElseThrow(() -> new IllegalArgumentException("Partido 'sigue a' no encontrado."));
            if (!parent.getCourt().equals(req.getCourt())) {
                throw new IllegalArgumentException("El partido 'sigue a' debe ser de la misma cancha.");
            }
            if (!parent.getMatchDate().equals(req.getMatchDate())) {
                throw new IllegalArgumentException("El partido 'sigue a' debe ser de la misma fecha.");
            }
        }

        log.info("[createMatch] creando partido {} vs {} en cancha {} orden {}",
            req.getPlayer1(), req.getPlayer2(), req.getCourt(), orderInCourt);

        return matchRepo.save(Match.builder()
            .matchDate(req.getMatchDate())
            .matchTime(req.getMatchTime())
            .court(req.getCourt())
            .player1(req.getPlayer1())
            .player2(req.getPlayer2())
            .round(req.getRound())
            .orderInCourt(orderInCourt)
            .followsMatchId(req.getFollowsMatchId())
            .status("SCHEDULED")
            .deadlineForced(false)
            .build());
    }

    @Transactional
    public MatchResultDto saveResult(Long matchId, MatchResultDto dto) {
        log.info("[saveResult] matchId={}, winner={}, setsWinner={}, gameResult={}",
            matchId, dto.getWinner(), dto.getSetsWinner(), dto.getGameResult());

        Match match = matchRepo.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado."));

        MatchResult res = resultRepo.findByMatchId(matchId)
            .orElse(MatchResult.builder().match(match).build());

        log.info("[saveResult] MatchResult existente? id={}", res.getId());

        res.setWinner(dto.getWinner());
        res.setSetsWinner(dto.getSetsWinner());
        res.setSetsLoser(dto.getSetsLoser());
        res.setGameResult(dto.getGameResult());

        res.setSet1W(dto.getSet1W());
        res.setSet1L(dto.getSet1L());
        res.setSet2W(dto.getSet2W());
        res.setSet2L(dto.getSet2L());
        res.setSet3W(dto.getSet3W());
        res.setSet3L(dto.getSet3L());
        res.setSet4W(dto.getSet4W());
        res.setSet4L(dto.getSet4L());
        res.setSet5W(dto.getSet5W());
        res.setSet5L(dto.getSet5L());

        res.setEnteredAt(LocalDateTime.now());
        resultRepo.save(res);

        log.info("[saveResult] MatchResult guardado con id={}", res.getId());

        if (!"FINISHED".equals(match.getStatus())) {
            match.setStatus("FINISHED");
            match.setActualEndTime(LocalDateTime.now());
            matchRepo.save(match);
            log.info("[saveResult] match {} marcado como FINISHED", matchId);

            courtQueueService.recalcularEstimadosEnCancha(match.getCourt(), match.getMatchDate());
        }

        return dto;
    }

    /**
     * NUEVO — Forzar cierre de pronóstico + pasar a IN_PLAY en un solo paso.
     * Como pediste: el admin cierra y el partido arranca.
     */
    @Transactional
    public Match forceDeadlineAndStart(Long matchId) {
        log.info("[forceDeadlineAndStart] matchId={}", matchId);

        Match m = matchRepo.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado."));

        if (!"SCHEDULED".equals(m.getStatus())) {
            throw new IllegalStateException(
                "Solo se puede cerrar un partido SCHEDULED. Status actual: " + m.getStatus());
        }

        m.setDeadlineForced(true);
        m.setStatus("IN_PLAY");
        if (m.getActualStartTime() == null) {
            m.setActualStartTime(LocalDateTime.now());
        }
        matchRepo.save(m);

        log.info("[forceDeadlineAndStart] ✓ match {} cerrado y en juego", matchId);
        return m;
    }

    /**
     * Solo cerrar pronóstico (sin cambiar status del partido).
     * El partido sigue SCHEDULED pero los usuarios no pueden editar.
     */
    @Transactional
    public Match forceDeadlineOnly(Long matchId) {
        log.info("[forceDeadlineOnly] matchId={}", matchId);

        Match m = matchRepo.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado."));

        if (m.getDeadlineForced()) {
            throw new IllegalStateException("El pronóstico ya está cerrado.");
        }

        m.setDeadlineForced(true);
        matchRepo.save(m);

        log.info("[forceDeadlineOnly] ✓ match {} pronóstico cerrado (sin cambiar status)", matchId);
        return m;
    }

    /**
     * NUEVO — Cargar score parcial durante el partido (sin winner, sin FINISHED).
     * El admin actualiza games en vivo; los usuarios lo ven en la card.
     */
    @Transactional
    public MatchResultDto updateLiveScore(Long matchId, MatchResultDto dto) {
        log.info("[updateLiveScore] === INICIO === matchId={}", matchId);
        log.info("[updateLiveScore] dto recibido: set1W={}, set1L={}, set2W={}, set2L={}, set3W={}, set3L={}, set4W={}, set4L={}, set5W={}, set5L={}, gameResult={}",
            dto.getSet1W(), dto.getSet1L(), dto.getSet2W(), dto.getSet2L(),
            dto.getSet3W(), dto.getSet3L(), dto.getSet4W(), dto.getSet4L(),
            dto.getSet5W(), dto.getSet5L(), dto.getGameResult());

        Match match = matchRepo.findById(matchId)
            .orElseThrow(() -> {
                log.error("[updateLiveScore] match {} NO ENCONTRADO en DB", matchId);
                return new IllegalArgumentException("Partido no encontrado.");
            });

        log.info("[updateLiveScore] match encontrado: id={}, status={}, p1={} vs p2={}, court={}",
            match.getId(), match.getStatus(), match.getPlayer1(), match.getPlayer2(), match.getCourt());

        if (!"IN_PLAY".equals(match.getStatus()) && !"SUSPENDED".equals(match.getStatus())) {
            log.error("[updateLiveScore] STATUS INVALIDO: {} (se requiere IN_PLAY o SUSPENDED)", match.getStatus());
            throw new IllegalStateException(
                "Solo se puede cargar score en vivo de un partido IN_PLAY o SUSPENDED. Status: "
                + match.getStatus());
        }

        MatchResult res = resultRepo.findByMatchId(matchId)
            .orElseGet(() -> {
                log.info("[updateLiveScore] No existe MatchResult para match {}. Creando nuevo...", matchId);
                return MatchResult.builder().match(match).build();
            });

        log.info("[updateLiveScore] MatchResult id={}, winner={}, existe={}", res.getId(), res.getWinner(), res.getId() != null);

        // Solo actualizamos los sets que vienen en el dto (los null no se tocan)
        if (dto.getSet1W() != null) { res.setSet1W(dto.getSet1W()); log.info("[updateLiveScore] set1W -> {}", dto.getSet1W()); }
        if (dto.getSet1L() != null) { res.setSet1L(dto.getSet1L()); log.info("[updateLiveScore] set1L -> {}", dto.getSet1L()); }
        if (dto.getSet2W() != null) { res.setSet2W(dto.getSet2W()); log.info("[updateLiveScore] set2W -> {}", dto.getSet2W()); }
        if (dto.getSet2L() != null) { res.setSet2L(dto.getSet2L()); log.info("[updateLiveScore] set2L -> {}", dto.getSet2L()); }
        if (dto.getSet3W() != null) { res.setSet3W(dto.getSet3W()); log.info("[updateLiveScore] set3W -> {}", dto.getSet3W()); }
        if (dto.getSet3L() != null) { res.setSet3L(dto.getSet3L()); log.info("[updateLiveScore] set3L -> {}", dto.getSet3L()); }
        if (dto.getSet4W() != null) { res.setSet4W(dto.getSet4W()); log.info("[updateLiveScore] set4W -> {}", dto.getSet4W()); }
        if (dto.getSet4L() != null) { res.setSet4L(dto.getSet4L()); log.info("[updateLiveScore] set4L -> {}", dto.getSet4L()); }
        if (dto.getSet5W() != null) { res.setSet5W(dto.getSet5W()); log.info("[updateLiveScore] set5W -> {}", dto.getSet5W()); }
        if (dto.getSet5L() != null) { res.setSet5L(dto.getSet5L()); log.info("[updateLiveScore] set5L -> {}", dto.getSet5L()); }

        // No tocamos winner ni setsWinner — eso se carga al finalizar
        if (dto.getGameResult() != null) res.setGameResult(dto.getGameResult());

        res.setEnteredAt(LocalDateTime.now());
        MatchResult saved = resultRepo.save(res);
        log.info("[updateLiveScore] ✓ MatchResult guardado OK id={}", saved.getId());
        return toDto(saved);
    }

    private MatchResultDto toDto(MatchResult r) {
        return MatchResultDto.builder()
            .winner(r.getWinner())
            .setsWinner(r.getSetsWinner())
            .setsLoser(r.getSetsLoser())
            .gameResult(r.getGameResult())
            .set1W(r.getSet1W()).set1L(r.getSet1L())
            .set2W(r.getSet2W()).set2L(r.getSet2L())
            .set3W(r.getSet3W()).set3L(r.getSet3L())
            .set4W(r.getSet4W()).set4L(r.getSet4L())
            .set5W(r.getSet5W()).set5L(r.getSet5L())
            .build();
    }

    @Transactional
    public Match updateStatus(Long matchId, String newStatus) {
        log.info("[updateStatus] matchId={}, nuevo={}", matchId, newStatus);

        Match m = matchRepo.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado."));

        String upper = newStatus.toUpperCase();
        switch (upper) {
            case "IN_PLAY":
                if (m.getActualStartTime() == null) m.setActualStartTime(LocalDateTime.now());
                break;
            case "SUSPENDED":
                break;
            case "FINISHED":
            case "WALKOVER":
            case "RETIRED":
            case "ABANDONED":
                m.setActualEndTime(LocalDateTime.now());
                if (resultRepo.findByMatchId(matchId).isEmpty()) {
                    log.warn("[updateStatus] match {} finalizado SIN resultado en match_results. " +
                             "Cargar resultado manualmente desde el admin.", matchId);
                }
                break;
            case "SCHEDULED":
                m.setActualStartTime(null);
                m.setActualEndTime(null);
                m.setDeadlineForced(false);
                break;
            default:
                throw new IllegalArgumentException("Status inválido: " + newStatus);
        }
        m.setStatus(upper);
        Match saved = matchRepo.save(m);

        if ("FINISHED".equals(upper) || "WALKOVER".equals(upper)
            || "RETIRED".equals(upper) || "ABANDONED".equals(upper)) {
            courtQueueService.recalcularEstimadosEnCancha(saved.getCourt(), saved.getMatchDate());
        }

        return saved;
    }

    public void deleteMatch(Long id) {
        Match m = matchRepo.findById(id).orElse(null);
        if (m != null) {
            courtQueueService.recadenarAlBorrar(id, m.getCourt(), m.getMatchDate());
        }
        matchRepo.deleteById(id);
    }
}

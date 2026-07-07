package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchAdminService {

    private final MatchRepository       matchRepo;
    private final MatchResultRepository resultRepo;
    private final CourtQueueService     courtQueueService;
    private final PickRepository        pickRepo;
    private final NotificationService   notificationService;

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
        log.info("[saveResult] === INICIO === matchId={}, winner={}, setsW={}, setsL={}, gameResult={}, retired={}",
            matchId, dto.getWinner(), dto.getSetsWinner(), dto.getSetsLoser(), dto.getGameResult(), dto.getRetired());

        try {
            Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado. id=" + matchId));
            log.info("[saveResult] match encontrado: id={}, status={}, player1={}, player2={}",
                match.getId(), match.getStatus(), match.getPlayer1(), match.getPlayer2());

            MatchResult res = resultRepo.findByMatchId(matchId)
                .orElse(MatchResult.builder().match(match).build());
            log.info("[saveResult] MatchResult {} (id={})", res.getId() == null ? "NUEVO" : "EXISTENTE", res.getId());

            res.setWinner(dto.getWinner());
            res.setSetsWinner(dto.getSetsWinner());
            res.setSetsLoser(dto.getSetsLoser());
            res.setGameResult(dto.getGameResult());

            log.info("[saveResult] seteando sets: s1={}-{} s2={}-{} s3={}-{} s4={}-{} s5={}-{}",
                dto.getSet1W(), dto.getSet1L(), dto.getSet2W(), dto.getSet2L(),
                dto.getSet3W(), dto.getSet3L(), dto.getSet4W(), dto.getSet4L(),
                dto.getSet5W(), dto.getSet5L());

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
            MatchResult saved = resultRepo.save(res);
            log.info("[saveResult] ✓ MatchResult guardado id={}", saved.getId());

            String newStatus = Boolean.TRUE.equals(dto.getRetired()) ? "RETIRED" : "FINISHED";
            log.info("[saveResult] nuevoStatus={} vs statusActual={}", newStatus, match.getStatus());
            if (!newStatus.equals(match.getStatus())) {
                match.setStatus(newStatus);
                match.setActualEndTime(LocalDateTime.now());
                matchRepo.save(match);
                log.info("[saveResult] ✓ match {} marcado como {}", matchId, newStatus);

                try {
                    courtQueueService.recalcularEstimadosEnCancha(match.getCourt(), match.getMatchDate());
                } catch (Exception e) {
                    log.error("[saveResult] error al recalcular estimados: {}", e.getMessage(), e);
                }

                try {
                    String winner = dto.getWinner() != null ? dto.getWinner() : "Resultado cargado";
                    sendMatchNotification(match, "Resultado: " + winner, match.getPlayer1() + " vs " + match.getPlayer2());
                } catch (Exception e) {
                    log.error("[saveResult] error al notificar: {}", e.getMessage(), e);
                }
            }

            log.info("[saveResult] === OK === matchId={}", matchId);
            return dto;

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[saveResult] === ERROR CONTROLADO === matchId={}: {}", matchId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[saveResult] === ERROR INESPERADO === matchId={}: {}", matchId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * FIX Req 3: Solo cierra el pronóstico (deadlineForced = true).
     * NO cambia el status del partido — el admin decide cuándo iniciar el partido por separado.
     */
    @Transactional
    public Match forceDeadline(Long matchId) {
        log.info("[forceDeadline] matchId={}", matchId);

        Match m = matchRepo.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado."));

        if (Boolean.TRUE.equals(m.getDeadlineForced())) {
            throw new IllegalStateException("El pronóstico ya está cerrado.");
        }

        m.setDeadlineForced(true);
        matchRepo.save(m);

        log.info("[forceDeadline] ✓ pronóstico cerrado para match {}", matchId);
        return m;
    }

    /**
     * Cerrar pronóstico + pasar a IN_PLAY en un solo paso (comportamiento anterior).
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

        // Notificar a los usuarios que hicieron pick en este partido
        sendMatchNotification(m, "Empieza el partido!", m.getPlayer1() + " vs " + m.getPlayer2() + " esta en juego");

        return m;
    }

    /**
     * Cargar score parcial durante el partido (sin winner, sin FINISHED).
     */
    @Transactional
    public MatchResultDto updateLiveScore(Long matchId, MatchResultDto dto) {
        log.info("[updateLiveScore] === INICIO === matchId={}, dto={}", matchId, dto);

        try {
            Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado. id=" + matchId));
            log.info("[updateLiveScore] match encontrado: status={}, player1={} vs player2={}",
                match.getStatus(), match.getPlayer1(), match.getPlayer2());

            if (!"IN_PLAY".equals(match.getStatus()) && !"SUSPENDED".equals(match.getStatus())) {
                throw new IllegalStateException(
                    "Solo se puede cargar score en vivo de un partido IN_PLAY o SUSPENDED. Status: "
                    + match.getStatus());
            }

            MatchResult res = resultRepo.findByMatchId(matchId)
                .orElse(MatchResult.builder().match(match).build());
            log.info("[updateLiveScore] MatchResult {} (id={})", res.getId() == null ? "NUEVO" : "EXISTENTE", res.getId());

            if (dto.getSet1W() != null) res.setSet1W(dto.getSet1W());
            if (dto.getSet1L() != null) res.setSet1L(dto.getSet1L());
            if (dto.getSet2W() != null) res.setSet2W(dto.getSet2W());
            if (dto.getSet2L() != null) res.setSet2L(dto.getSet2L());
            if (dto.getSet3W() != null) res.setSet3W(dto.getSet3W());
            if (dto.getSet3L() != null) res.setSet3L(dto.getSet3L());
            if (dto.getSet4W() != null) res.setSet4W(dto.getSet4W());
            if (dto.getSet4L() != null) res.setSet4L(dto.getSet4L());
            if (dto.getSet5W() != null) res.setSet5W(dto.getSet5W());
            if (dto.getSet5L() != null) res.setSet5L(dto.getSet5L());

            if (dto.getGameResult() != null) res.setGameResult(dto.getGameResult());

            res.setEnteredAt(LocalDateTime.now());
            MatchResult saved = resultRepo.save(res);

            log.info("[updateLiveScore] === OK === matchResultId={}", saved.getId());
            return toDto(res);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[updateLiveScore] === ERROR CONTROLADO === matchId={}: {}", matchId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[updateLiveScore] === ERROR INESPERADO === matchId={}: {}", matchId, e.getMessage(), e);
            throw e;
        }
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

    @Async
    public void sendMatchNotification(Match match, String title, String body) {
        try {
            List<Pick> picks = pickRepo.findByMatchId(match.getId());
            log.info("[sendMatchNotification] {} picks para match {}", picks.size(), match.getId());
            for (Pick p : picks) {
                notificationService.sendToUser(p.getUser().getId(), title, body);
            }
        } catch (Exception e) {
            log.error("[sendMatchNotification] error: {}", e.getMessage());
        }
    }
}

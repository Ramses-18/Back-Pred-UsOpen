package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

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

        // FIX BUG 2: guardar null en vez de 0 para sets no cargados
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

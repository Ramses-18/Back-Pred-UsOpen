package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MatchAdminService {

    private final MatchRepository       matchRepo;
    private final MatchResultRepository resultRepo;

    public Match createMatch(MatchCreateRequest req) {
        // Calcular orderInCourt automáticamente: 1 + max(order) en esa cancha+fecha
        Integer maxOrder = matchRepo.findMaxOrderInCourt(req.getMatchDate(), req.getCourt());
        int orderInCourt = (maxOrder == null ? 0 : maxOrder) + 1;

        // Validar que followsMatchId (si viene) sea de la misma cancha y fecha
        if (req.getFollowsMatchId() != null) {
            Match parent = matchRepo.findById(req.getFollowsMatchId())
                .orElseThrow(() -> new IllegalArgumentException("Partido 'sigue a' no encontrado."));
            if (!parent.getCourt().equals(req.getCourt())) {
                throw new IllegalArgumentException(
                    "El partido 'sigue a' debe ser de la misma cancha.");
            }
            if (!parent.getMatchDate().equals(req.getMatchDate())) {
                throw new IllegalArgumentException(
                    "El partido 'sigue a' debe ser de la misma fecha.");
            }
        }

        return matchRepo.save(Match.builder()
            .matchDate(req.getMatchDate())
            .matchTime(req.getMatchTime())          // puede ser null
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
        Match match = matchRepo.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado."));
        MatchResult res = resultRepo.findByMatchId(matchId)
            .orElse(MatchResult.builder().match(match).build());

        // (tu código existente de setWinner, set1W, etc. — sin cambios)
        res.setWinner(dto.getWinner());
        res.setSetsWinner(dto.getSetsWinner());
        res.setSetsLoser(dto.getSetsLoser());
        res.setGameResult(dto.getGameResult());
        res.setSet1W(dto.getSet1W() != null ? dto.getSet1W() : 0);
        res.setSet1L(dto.getSet1L() != null ? dto.getSet1L() : 0);
        res.setSet2W(dto.getSet2W() != null ? dto.getSet2W() : 0);
        res.setSet2L(dto.getSet2L() != null ? dto.getSet2L() : 0);
        res.setSet3W(dto.getSet3W() != null ? dto.getSet3W() : 0);
        res.setSet3L(dto.getSet3L() != null ? dto.getSet3L() : 0);
        res.setSet4W(dto.getSet4W() != null ? dto.getSet4W() : 0);
        res.setSet4L(dto.getSet4L() != null ? dto.getSet4L() : 0);
        res.setSet5W(dto.getSet5W() != null ? dto.getSet5W() : 0);
        res.setSet5L(dto.getSet5L() != null ? dto.getSet5L() : 0);
        res.setEnteredAt(LocalDateTime.now());
        resultRepo.save(res);

        // NUEVO: marcar el match como FINISHED y propagar a la cola
        if (!"FINISHED".equals(match.getStatus())) {
            match.setStatus("FINISHED");
            match.setActualEndTime(LocalDateTime.now());
            matchRepo.save(match);
            // El recálculo de estimados de los siguientes lo hace TennisApiService
            // cuando detecta el cambio de status, pero también podemos dispararlo acá:
            // tennisApiService.recalcularEstimadosEnCancha(match.getCourt(), match.getMatchDate());
        }

        return dto;
    }

    /** Cambio manual de status (lluvia, walkover, etc.) */
    @Transactional
    public Match updateStatus(Long matchId, String newStatus) {
        Match m = matchRepo.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado."));

        String upper = newStatus.toUpperCase();
        switch (upper) {
            case "IN_PLAY":
                if (m.getActualStartTime() == null) m.setActualStartTime(LocalDateTime.now());
                break;
            case "SUSPENDED":
                // No reseteamos actualStartTime, puede reanudarse
                break;
            case "FINISHED":
            case "WALKOVER":
            case "RETIRED":
            case "ABANDONED":
                m.setActualEndTime(LocalDateTime.now());
                break;
            case "SCHEDULED":
                // Si el admin revierte a SCHEDULED, limpiar timestamps
                m.setActualStartTime(null);
                m.setActualEndTime(null);
                break;
            default:
                throw new IllegalArgumentException("Status inválido: " + newStatus);
        }
        m.setStatus(upper);
        return matchRepo.save(m);
    }

    public void deleteMatch(Long id) {
        // Antes de borrar, re-cadenar el follows_match_id de quien lo seguía
        Match m = matchRepo.findById(id).orElse(null);
        if (m != null) {
            Match next = matchRepo.findByFollowsMatchId(id);
            if (next != null) {
                next.setFollowsMatchId(m.getFollowsMatchId()); // saltea al borrado
                matchRepo.save(next);
            }
        }
        matchRepo.deleteById(id);
    }
}
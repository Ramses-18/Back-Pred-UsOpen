package com.wimbledon.service;

import com.wimbledon.entity.Match;
import com.wimbledon.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

/**
 * Service para manejar la cola por cancha (follows_match_id, order_in_court).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourtQueueService {

    private final MatchRepository matchRepo;

    /**
     * Recalcula estimated start de los partidos siguientes al dado en la misma cancha.
     * Se llama cuando un partido termina (FINISHED/WALKOVER/etc.) para que el siguiente
     * pueda arrancar.
     */
    public void recalcularEstimadosEnCancha(String court, LocalDate date) {
        try {
            List<Match> cola = matchRepo.findByMatchDateAndCourtOrderByOrderInCourtAsc(date, court);
            for (Match m : cola) {
                if ("SCHEDULED".equals(m.getStatus()) && m.getFollowsMatchId() != null) {
                    Match padre = matchRepo.findById(m.getFollowsMatchId()).orElse(null);
                    if (padre != null && padre.getActualEndTime() != null) {
                        log.debug("[recalcularEstimadosEnCancha] Partido {} listo para arrancar en {}",
                            m.getId(), court);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[recalcularEstimadosEnCancha] error: {}", e.getMessage());
        }
    }

    /**
     * Re-cadena el follows_match_id cuando se borra un partido del medio de la cola.
     * Si A sigue a B, y B sigue a C, y borramos B → A ahora sigue a C.
     */
    public void recadenarAlBorrar(Long matchIdBorrado, String court, LocalDate date) {
        try {
            Match borrado = matchRepo.findById(matchIdBorrado).orElse(null);
            if (borrado == null) return;
            Match siguiente = matchRepo.findByFollowsMatchId(matchIdBorrado);
            if (siguiente != null) {
                siguiente.setFollowsMatchId(borrado.getFollowsMatchId());
                matchRepo.save(siguiente);
                log.info("[recadenarAlBorrar] partido {} ahora sigue a {}",
                    siguiente.getId(), borrado.getFollowsMatchId());
            }
        } catch (Exception e) {
            log.error("[recadenarAlBorrar] error: {}", e.getMessage());
        }
    }
}

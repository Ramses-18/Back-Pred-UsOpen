package com.wimbledon.service;

import com.wimbledon.entity.Match;
import com.wimbledon.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourtQueueService {

    private final MatchRepository matchRepo;

    /** Recalcula estimated start de los partidos siguientes al dado en la misma cancha. */
    public void recalcularEstimadosEnCancha(String court, LocalDate date) {
        List<Match> cola = matchRepo.findByMatchDateAndCourtOrderByOrderInCourtAsc(date, court);
        for (Match m : cola) {
            if ("SCHEDULED".equals(m.getStatus()) && m.getFollowsMatchId() != null) {
                Match padre = matchRepo.findById(m.getFollowsMatchId()).orElse(null);
                if (padre != null && padre.getActualEndTime() != null) {
                    log.debug("Partido {} listo para arrancar en {}", m.getId(), court);
                }
            }
        }
    }

    /** Re-cadena el follows_match_id cuando se borra un partido del medio de la cola. */
    public void recadenarAlBorrar(Long matchIdBorrado, String court, LocalDate date) {
        Match borrado = matchRepo.findById(matchIdBorrado).orElse(null);
        if (borrado == null) return;
        Match siguiente = matchRepo.findByFollowsMatchId(matchIdBorrado);
        if (siguiente != null) {
            siguiente.setFollowsMatchId(borrado.getFollowsMatchId());
            matchRepo.save(siguiente);
        }
    }
}
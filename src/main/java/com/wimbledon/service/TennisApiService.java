package com.wimbledon.service;

import com.wimbledon.dto.MatchResultDto;
import com.wimbledon.entity.Match;
import com.wimbledon.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TennisApiService {

    @Value("${app.tennis-api.key:}")
    private String apiKey;

    @Value("${app.tennis-api.host:tennis-api-atp-wta-itf.p.rapidapi.com}")
    private String apiHost;

    @Value("${app.tennis-api.base-url:https://tennis-api-atp-wta-itf.p.rapidapi.com}")
    private String baseUrl;

    private final MatchRepository matchRepo;
    private final RestTemplate restTemplate;

    /**
     * Cada 2 minutos — trae partidos en vivo de la API y los inserta en DB
     * si no existen. NO cambia status ni guarda resultados (eso lo hace el admin).
     */
    @Scheduled(fixedDelay = 120_000)
    public void syncLiveEvents() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Buenos_Aires"));
        String url = baseUrl + "/tennis/v2/extend/api/events/live";

        HttpHeaders headers = apiHeaders();
        log.info("[syncLiveEvents] === INICIO === consultando {}", url);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (response.getBody() == null) {
                log.info("[syncLiveEvents] respuesta vacía");
                return;
            }

            Object resultsObj = response.getBody().get("results");
            if (!(resultsObj instanceof List)) {
                log.info("[syncLiveEvents] no hay lista 'results'");
                return;
            }

            List<Map<String, Object>> events = (List<Map<String, Object>>) resultsObj;
            List<Match> dbMatches = matchRepo.findByMatchDateOrderByMatchTimeAsc(today);

            List<Map<String, Object>> wimbledon = events.stream()
                    .filter(e -> {
                        String league = String.valueOf(e.get("league"));
                        String tourType = String.valueOf(e.get("tourType"));
                        return league.toLowerCase().contains("wimbledon")
                                && "atp".equalsIgnoreCase(tourType);
                    })
                    .collect(Collectors.toList());

            log.info("[syncLiveEvents] eventos live totales: {}, Wimbledon ATP: {}",
                    events.size(), wimbledon.size());

            int creados = 0;
            int yaExistian = 0;

            for (Map<String, Object> event : wimbledon) {
                String p1Name = String.valueOf(event.get("participant1"));
                String p2Name = String.valueOf(event.get("participant2"));

                final String p1Final = p1Name;
                final String p2Final = p2Name;

                boolean existeEnDB = dbMatches.stream()
                        .anyMatch(m -> coincidePartido(m, p1Final, p2Final));

                if (!existeEnDB) {
                    LocalTime matchTime = LocalTime.of(12, 0);
                    Object ts = event.get("startTimestamp");
                    if (ts != null) {
                        try {
                            long epoch = Long.parseLong(ts.toString());
                            matchTime = Instant.ofEpochSecond(epoch)
                                    .atZone(ZoneId.of("Europe/London"))
                                    .toLocalTime();
                        } catch (Exception ignored) {}
                    }

                    String court = "Wimbledon - London";
                    Object venue = event.get("venue");
                    if (venue != null && !"null".equals(venue.toString())) {
                        court = venue.toString();
                    }

                    Integer maxOrder = matchRepo.findMaxOrderInCourt(today, court);
                    int orderInCourt = (maxOrder == null ? 0 : maxOrder) + 1;

                    Match nuevo = Match.builder()
                            .matchDate(today)
                            .matchTime(matchTime)
                            .court(court)
                            .player1(p1Name)
                            .player2(p2Name)
                            .round(parseRound(event.get("round")))
                            .orderInCourt(orderInCourt)
                            .followsMatchId(null)
                            .status("SCHEDULED")
                            .build();
                    matchRepo.save(nuevo);
                    dbMatches.add(nuevo);
                    creados++;
                    log.info("[syncLiveEvents] ✓ Partido creado: {} vs {} en {} (orden #{})",
                            p1Name, p2Name, court, orderInCourt);
                } else {
                    yaExistian++;
                }
            }

            log.info("[syncLiveEvents] === FIN === creados: {}, ya existían: {}",
                    creados, yaExistian);

        } catch (Exception e) {
            log.error("[syncLiveEvents] error: {}", e.getMessage(), e);
        }
    }

    private boolean coincidePartido(Match m, String apiP1, String apiP2) {
        String l1db = apellido(m.getPlayer1());
        String l2db = apellido(m.getPlayer2());
        String l1api = apellido(apiP1);
        String l2api = apellido(apiP2);
        return (l1db.equalsIgnoreCase(l1api) && l2db.equalsIgnoreCase(l2api))
                || (l1db.equalsIgnoreCase(l2api) && l2db.equalsIgnoreCase(l1api));
    }

    private String apellido(String nombre) {
        if (nombre == null || nombre.isBlank()) return "";
        String[] p = nombre.trim().split("\\s+");
        return p[p.length - 1].toLowerCase();
    }

    private String parseRound(Object round) {
        if (round == null) return null;
        String s = round.toString();
        if (s.contains("128")) return "R128";
        if (s.contains("64"))  return "R64";
        if (s.contains("32"))  return "R32";
        if (s.contains("16"))  return "R16";
        if (s.toLowerCase().contains("quarter") || s.contains("1/4")) return "QF";
        if (s.toLowerCase().contains("semi") || s.contains("1/2"))    return "SF";
        if (s.toLowerCase().contains("final") && !s.contains("semi")) return "F";
        return s;
    }

    private HttpHeaders apiHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-RapidAPI-Key", apiKey);
        h.set("X-RapidAPI-Host", apiHost);
        return h;
    }
}

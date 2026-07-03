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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

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
    private final MatchAdminService matchAdminService;
    private final CourtQueueService courtQueueService;
    private final RestTemplate restTemplate;

    /**
     * Cada 2 min — actualiza status y scores de partidos en vivo.
     * Para partidos cargados manualmente (sin apiEventId), busca el eventId
     * vía /event/get/{player1}/{player2}/{date} y lo linkea.
     */
    @Scheduled(fixedDelay = 120_000)
    public void syncLiveStatus() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));
        List<Match> scheduledOrLive = matchRepo.findByMatchDateAndStatusIn(
            today, List.of("SCHEDULED", "IN_PLAY", "SUSPENDED"));

        log.info("[syncLive] === INICIO === partidos a revisar: {}", scheduledOrLive.size());

        int sinApiEventId = 0;
        int linkeados = 0;
        int actualizados = 0;

        for (Match m : scheduledOrLive) {
            if (m.getApiEventId() == null) {
                sinApiEventId++;
                // Intentar linkear con la API
                String apiEventId = fetchApiEventId(m.getPlayer1(), m.getPlayer2(), m.getMatchDate());
                if (apiEventId == null) {
                    log.debug("[syncLive] match {} ({} vs {}) sin eventId en API todavía",
                        m.getId(), m.getPlayer1(), m.getPlayer2());
                    continue;
                }
                m.setApiEventId(apiEventId);
                matchRepo.save(m);
                linkeados++;
                log.info("[syncLive] ✓ match {} linkeado con apiEventId={}",
                    m.getId(), apiEventId);
            }

            try {
                Optional<Map<String, Object>> liveEvent = findLiveEventById(m.getApiEventId());
                if (liveEvent.isEmpty()) {
                    // No está en live todavía → sigue SCHEDULED
                    continue;
                }

                Map<String, Object> event = liveEvent.get();
                String status = String.valueOf(event.get("status")); // "InPlay" | "Finished"
                String score = String.valueOf(event.get("score"));
                String indicator = String.valueOf(event.get("indicator"));

                // Transición SCHEDULED -> IN_PLAY
                if ("InPlay".equalsIgnoreCase(status) && "SCHEDULED".equals(m.getStatus())) {
                    m.setActualStartTime(LocalDateTime.now());
                    m.setStatus("IN_PLAY");
                    matchRepo.save(m);
                    actualizados++;
                    log.info("[syncLive] 🎾 Arrancó: {} vs {} en {}",
                        m.getPlayer1(), m.getPlayer2(), m.getCourt());
                }

                // Transición a FINISHED
                if ("Finished".equalsIgnoreCase(status) && !"FINISHED".equals(m.getStatus())) {
                    log.info("[syncLive] detectado FINISHED para match {} ({} vs {})",
                        m.getId(), m.getPlayer1(), m.getPlayer2());

                    MatchResultDto dto = parsearResultadoLive(m, m.getPlayer1(), m.getPlayer2(), score, indicator);
                    if (dto != null) {
                        matchAdminService.saveResult(m.getId(), dto);
                        actualizados++;
                        log.info("[syncLive] ✓ Resultado guardado: {} vs {} | {} | ganador: {}",
                            m.getPlayer1(), m.getPlayer2(), score, dto.getWinner());
                    } else {
                        log.error("[syncLive] no se pudo parsear resultado para match {}: score='{}'",
                            m.getId(), score);
                        // Igual marcar como FINISHED para que no se quede colgado
                        m.setStatus("FINISHED");
                        m.setActualEndTime(LocalDateTime.now());
                        matchRepo.save(m);
                    }
                }
            } catch (Exception e) {
                log.error("[syncLive] error en match {} (apiEventId={}): {}",
                    m.getId(), m.getApiEventId(), e.getMessage());
            }
        }

        log.info("[syncLive] === FIN === sin eventId: {}, linkeados: {}, actualizados: {}",
            sinApiEventId, linkeados, actualizados);
    }

    /**
     * Busca el eventId en matchstat por jugadores + fecha.
     * Endpoint: GET /event/get/{player1}/{player2}/{date}
     */
    public String fetchApiEventId(String player1, String player2, LocalDate date) {
        try {
            // matchstat espera apellidos o nombres completos (probar con apellido)
            String p1 = lastName(player1);
            String p2 = lastName(player2);
            String url = String.format("%s/tennis/v2/extend/api/event/get/%s/%s/%s",
                baseUrl,
                URLEncoder.encode(p1, StandardCharsets.UTF_8),
                URLEncoder.encode(p2, StandardCharsets.UTF_8),
                date.toString());

            log.debug("[fetchApiEventId] GET {}", url);

            ResponseEntity<Map> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(apiHeaders()), Map.class);

            if (resp.getBody() == null) return null;
            Object results = resp.getBody().get("results");
            if (!(results instanceof List)) return null;
            List<?> list = (List<?>) results;
            if (list.isEmpty()) return null;

            Map<String, Object> first = (Map<String, Object>) list.get(0);
            Object id = first.get("event_id");
            if (id == null) id = first.get("id");
            return id == null ? null : String.valueOf(id);
        } catch (Exception e) {
            log.debug("[fetchApiEventId] no encontrado para {} vs {}: {}", player1, player2, e.getMessage());
            return null;
        }
    }

    /**
     * Busca un evento en /events/live por apiEventId.
     */
    private Optional<Map<String, Object>> findLiveEventById(String apiEventId) {
        try {
            String url = baseUrl + "/tennis/v2/extend/api/events/live";
            ResponseEntity<Map> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(apiHeaders()), Map.class);
            if (resp.getBody() == null) return Optional.empty();
            Object resultsObj = resp.getBody().get("results");
            if (!(resultsObj instanceof List)) return Optional.empty();
            List<Map<String, Object>> events = (List<Map<String, Object>>) resultsObj;

            return events.stream().filter(e -> {
                Object id = e.get("event_id");
                if (id == null) id = e.get("id");
                return id != null && apiEventId.equals(String.valueOf(id));
            }).findFirst();
        } catch (Exception e) {
            log.error("[findLiveEventById] error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parsea score "7-6,4-6,6-3" con indicator "1,0" (participant1 ganó)
     * o "0,1" (participant2 ganó).
     */
    private MatchResultDto parsearResultadoLive(Match match, String p1, String p2,
            String score, String indicator) {
        try {
            if (score == null || "null".equals(score) || score.isEmpty()) {
                log.warn("[parsearResultadoLive] score vacío para match {}", match.getId());
                return null;
            }

            boolean p1Gano = indicator != null && indicator.startsWith("1");
            String winnerName = p1Gano ? match.getPlayer1() : match.getPlayer2();

            String[] sets = score.split(",");
            int setsGanador = 0;
            int[] setW = new int[5];
            int[] setL = new int[5];

            for (int i = 0; i < sets.length && i < 5; i++) {
                String set = sets[i].trim().replaceAll("\\(.*\\)", "");
                if (set.contains("-")) {
                    String[] parts = set.split("-");
                    if (parts.length == 2) {
                        try {
                            int a = Integer.parseInt(parts[0].trim());
                            int b = Integer.parseInt(parts[1].trim());
                            setW[i] = p1Gano ? a : b;
                            setL[i] = p1Gano ? b : a;
                            if (setW[i] > setL[i]) setsGanador++;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            MatchResultDto dto = MatchResultDto.builder()
                .winner(winnerName)
                .setsWinner(setsGanador > 0 ? setsGanador : null)
                .gameResult(score)
                .set1W(sets.length > 0 ? setW[0] : null)
                .set1L(sets.length > 0 ? setL[0] : null)
                .set2W(sets.length > 1 ? setW[1] : null)
                .set2L(sets.length > 1 ? setL[1] : null)
                .set3W(sets.length > 2 ? setW[2] : null)
                .set3L(sets.length > 2 ? setL[2] : null)
                .set4W(sets.length > 3 ? setW[3] : null)
                .set4L(sets.length > 3 ? setL[3] : null)
                .set5W(sets.length > 4 ? setW[4] : null)
                .set5L(sets.length > 4 ? setL[4] : null)
                .build();

            log.info("[parsearResultadoLive] match {}: winner={}, sets={}, score={}",
                match.getId(), winnerName, setsGanador, score);
            return dto;
        } catch (Exception e) {
            log.error("[parsearResultadoLive] error parseando '{}': {}", score, e.getMessage());
            return null;
        }
    }

    private HttpHeaders apiHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-RapidAPI-Key", apiKey);
        h.set("X-RapidAPI-Host", apiHost);
        return h;
    }

    private String lastName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }
}
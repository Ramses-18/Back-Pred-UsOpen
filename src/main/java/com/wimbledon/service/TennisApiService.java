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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TennisApiService {

    @Value("${app.tennis-api.key}")
    private String apiKey;

    @Value("${app.tennis-api.host}")
    private String apiHost;

    private final MatchRepository matchRepo;
    private final MatchAdminService matchAdminService;
    private final RestTemplate restTemplate;

    @Scheduled(fixedDelay = 120_000) // cada 2 minutos — es live
    public void syncTodayResults() {
        String url = "https://" + apiHost + "/tennis/v2/extend/api/events/live";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key", apiKey);
        headers.set("X-RapidAPI-Host", apiHost);

        log.info("Sincronizando scores en vivo - Wimbledon");

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class);

            if (response.getBody() == null)
                return;

            Object resultsObj = response.getBody().get("results");
            if (!(resultsObj instanceof List))
                return;

            List<Map<String, Object>> events = (List<Map<String, Object>>) resultsObj;
            List<Match> dbMatches = matchRepo.findByMatchDateOrderByMatchTimeAsc(LocalDate.now());

            // Filtrar solo Wimbledon ATP masculino
            List<Map<String, Object>> wimbledon = events.stream()
                    .filter(e -> {
                        String league = String.valueOf(e.get("league"));
                        String tourType = String.valueOf(e.get("tourType"));
                        return league.toLowerCase().contains("wimbledon")
                                && "atp".equalsIgnoreCase(tourType);
                    })
                    .collect(java.util.stream.Collectors.toList());

            log.info("Eventos Wimbledon ATP en vivo: {}", wimbledon.size());

            for (Map<String, Object> event : wimbledon) {
                String p1Name = String.valueOf(event.get("participant1"));
                String p2Name = String.valueOf(event.get("participant2"));
                String score = String.valueOf(event.get("score")); // "7-6,4-6,4-2"
                String status = String.valueOf(event.get("status")); // "InPlay" o "Finished"
                String indicator = String.valueOf(event.get("indicator")); // "1,0" o "0,1"

                final String p1Final = p1Name;
                final String p2Final = p2Name;

                // Crear partido si no existe en DB
                boolean existeEnDB = dbMatches.stream()
                        .anyMatch(m -> coincidePartido(m, p1Final, p2Final));

                if (!existeEnDB) {
                    Object ts = event.get("startTimestamp");
                    LocalTime matchTime = LocalTime.of(12, 0);
                    if (ts != null) {
                        try {
                            long epoch = Long.parseLong(ts.toString());
                            matchTime = java.time.Instant.ofEpochSecond(epoch)
                                    .atZone(java.time.ZoneId.of("Europe/London"))
                                    .toLocalTime();
                        } catch (Exception ignored) {
                        }
                    }
                    Match nuevo = Match.builder()
                            .matchDate(LocalDate.now())
                            .matchTime(matchTime)
                            .court("Wimbledon - London")
                            .player1(p1Name)
                            .player2(p2Name)
                            .round(null)
                            .build();
                    matchRepo.save(nuevo);
                    dbMatches.add(nuevo);
                    log.info("✓ Partido creado: {} vs {}", p1Name, p2Name);
                }

                // Guardar resultado solo si terminó
                if (!"Finished".equalsIgnoreCase(status))
                    continue;

                dbMatches.stream()
                        .filter(m -> coincidePartido(m, p1Final, p2Final))
                        .findFirst()
                        .ifPresent(m -> {
                            MatchResultDto dto = parsearResultadoLive(m, p1Final, p2Final, score, indicator);
                            if (dto != null) {
                                matchAdminService.saveResult(m.getId(), dto);
                                log.info("✓ Resultado final: {} vs {} | {} | ganador: {}",
                                        p1Final, p2Final, score, dto.getWinner());
                            }
                        });
            }

        } catch (Exception e) {
            log.error("Error sincronizando live: {}", e.getMessage(), e);
        }
    }

    /**
     * Parsea score "7-6,4-6,6-3" con indicator "1,0" (participant1 ganó)
     * o "0,1" (participant2 ganó)
     */
    private MatchResultDto parsearResultadoLive(Match match, String p1, String p2,
            String score, String indicator) {
        try {
            // indicator "1,0" = p1 ganó | "0,1" = p2 ganó
            boolean p1Gano = indicator.startsWith("1");
            String winnerName = p1Gano ? match.getPlayer1() : match.getPlayer2();

            // Parsear sets: "7-6,4-6,6-3"
            String[] sets = score.split(",");
            int setsGanador = 0;
            int[] setW = new int[5];
            int[] setL = new int[5];
            int totalGW = 0, totalGL = 0;

            for (int i = 0; i < sets.length && i < 5; i++) {
                String set = sets[i].trim().replaceAll("\\(.*\\)", "");
                if (set.contains("-")) {
                    String[] parts = set.split("-");
                    if (parts.length == 2) {
                        try {
                            int a = Integer.parseInt(parts[0].trim()); // participant1
                            int b = Integer.parseInt(parts[1].trim()); // participant2

                            // setW = games del ganador, setL = games del perdedor
                            setW[i] = p1Gano ? a : b;
                            setL[i] = p1Gano ? b : a;
                            totalGW += setW[i];
                            totalGL += setL[i];

                            // Contar sets ganados por el ganador
                            if (setW[i] > setL[i])
                                setsGanador++;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            return MatchResultDto.builder()
                    .winner(winnerName)
                    .setsWinner(setsGanador > 0 ? setsGanador : null)
                    .gamesWinner(totalGW > 0 ? totalGW : null)
                    .gamesLoser(totalGL > 0 ? totalGL : null)
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

        } catch (Exception e) {
            log.error("Error parseando resultado live '{}': {}", score, e.getMessage());
            return null;
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
        if (nombre == null || nombre.isBlank())
            return "";
        String[] p = nombre.trim().split("\\s+");
        return p[p.length - 1].toLowerCase();
    }
}

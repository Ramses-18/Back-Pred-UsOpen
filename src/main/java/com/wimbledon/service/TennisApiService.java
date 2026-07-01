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

    private final MatchRepository   matchRepo;
    private final MatchAdminService matchAdminService;
    private final RestTemplate      restTemplate;

    @Scheduled(fixedDelay = 600_000)
    public void syncTodayResults() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String url = "https://" + apiHost
            + "/tennis/v2/atp/fixtures/" + today
            + "?filter=PlayerGroup:singles&pageSize=100&include=tournament,round";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key",  apiKey);
        headers.set("X-RapidAPI-Host", apiHost);

        log.info("Sincronizando partidos y resultados ATP - {}", today);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(headers), Map.class
            );

            if (response.getBody() == null) return;

            Object dataObj = response.getBody().get("data");
            if (!(dataObj instanceof List)) return;

            List<Map<String, Object>> fixtures = (List<Map<String, Object>>) dataObj;
            List<Match> dbMatches = matchRepo.findByMatchDateOrderByMatchTimeAsc(LocalDate.now());

            log.info("Fixtures API: {} | Partidos en DB hoy: {}", fixtures.size(), dbMatches.size());

            for (Map<String, Object> fixture : fixtures) {

                Map<String, Object> p1 = (Map<String, Object>) fixture.get("player1");
                Map<String, Object> p2 = (Map<String, Object>) fixture.get("player2");
                if (p1 == null || p2 == null) continue;

                String player1Name = (String) p1.get("name");
                String player2Name = (String) p2.get("name");
                if (player1Name == null || player2Name == null) continue;

                // Ignorar dobles
                if (player1Name.contains("/") || player2Name.contains("/")) continue;

                // Solo Wimbledon
                String court = "Wimbledon";
                String round = null;
                Map<String, Object> tournament = (Map<String, Object>) fixture.get("tournament");
                if (tournament != null) {
                    String tName = String.valueOf(tournament.get("name"));
                    if (!tName.toLowerCase().contains("wimbledon")) continue;
                    court = tName;
                }
                Map<String, Object> roundObj = (Map<String, Object>) fixture.get("round");
                if (roundObj != null) {
                    Object rName = roundObj.get("name");
                    if (rName != null) round = rName.toString();
                }

                // Hora del partido
                LocalTime matchTime = LocalTime.of(12, 0);
                Object dateObj = fixture.get("date");
                if (dateObj != null) {
                    try {
                        LocalDateTime dt = LocalDateTime.parse(
                            dateObj.toString().replace("Z", ""),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        );
                        matchTime = dt.toLocalTime();
                    } catch (Exception ignored) {}
                }

                final String p1Final = player1Name;
                final String p2Final = player2Name;

                // Crear partido si no existe
                boolean existeEnDB = dbMatches.stream()
                    .anyMatch(m -> coincidePartido(m, p1Final, p2Final));

                if (!existeEnDB) {
                    Match nuevoPartido = Match.builder()
                        .matchDate(LocalDate.now())
                        .matchTime(matchTime)
                        .court(court)
                        .player1(player1Name)
                        .player2(player2Name)
                        .round(round)
                        .build();
                    matchRepo.save(nuevoPartido);
                    dbMatches.add(nuevoPartido);
                    log.info("✓ Partido creado: {} vs {}", player1Name, player2Name);
                }

                // Guardar resultado si terminó
                String result = (String) fixture.get("result");
                if (result == null || result.isBlank()) continue;

                dbMatches.stream()
                    .filter(m -> coincidePartido(m, p1Final, p2Final))
                    .findFirst()
                    .ifPresent(m -> {
                        MatchResultDto dto = parsearResultado(m, p1Final, result);
                        if (dto != null) {
                            matchAdminService.saveResult(m.getId(), dto);
                            log.info("✓ Resultado: {} derrotó a {} ({})",
                                p1Final, p2Final, result);
                        }
                    });
            }

        } catch (Exception e) {
            log.error("Error sincronizando: {}", e.getMessage(), e);
        }
    }

    /**
     * Parsea "6-3 6-4" o "6-3 4-6 6-2" y guarda sets individuales.
     * player1 de la API = ganador.
     */
    private MatchResultDto parsearResultado(Match match, String winnerName, String result) {
        try {
            String[] sets = result.trim().split("\\s+");
            int setsGanados = sets.length;

            // Arrays para scores por set [ganador, perdedor]
            int[] setW = new int[5];
            int[] setL = new int[5];
            int totalGamesW = 0, totalGamesL = 0;

            for (int i = 0; i < sets.length && i < 5; i++) {
                String setLimpio = sets[i].replaceAll("\\(.*\\)", "").trim();
                if (setLimpio.contains("-")) {
                    String[] games = setLimpio.split("-");
                    if (games.length == 2) {
                        try {
                            setW[i] = Integer.parseInt(games[0].trim());
                            setL[i] = Integer.parseInt(games[1].trim());
                            totalGamesW += setW[i];
                            totalGamesL += setL[i];
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // Determinar nombre del ganador en nuestra DB
            boolean p1EsGanador = apellido(winnerName).equalsIgnoreCase(apellido(match.getPlayer1()));
            String ganadorEnDB  = p1EsGanador ? match.getPlayer1() : match.getPlayer2();

            // Si ganó el player2, invertir scores de sets
            if (!p1EsGanador) {
                for (int i = 0; i < 5; i++) {
                    int tmp = setW[i]; setW[i] = setL[i]; setL[i] = tmp;
                }
                int tmp = totalGamesW; totalGamesW = totalGamesL; totalGamesL = tmp;
            }

            return MatchResultDto.builder()
                .winner(ganadorEnDB)
                .setsWinner(setsGanados)
                .gamesWinner(totalGamesW > 0 ? totalGamesW : null)
                .gamesLoser(totalGamesL  > 0 ? totalGamesL  : null)
                .gameResult(result)
                // Sets individuales
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
            log.error("Error parseando '{}': {}", result, e.getMessage());
            return null;
        }
    }

    private boolean coincidePartido(Match m, String apiP1, String apiP2) {
        String l1db  = apellido(m.getPlayer1());
        String l2db  = apellido(m.getPlayer2());
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
}

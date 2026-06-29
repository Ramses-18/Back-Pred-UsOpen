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

    @Scheduled(fixedDelay = 600_000) // cada 10 minutos
    public void syncTodayResults() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String url   = "https://" + apiHost + "/api/tennis/atp-singles/" + today;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key",  apiKey);
        headers.set("x-rapidapi-host", apiHost);
        headers.set("Content-Type", "application/json");

        log.info("Sincronizando resultados ATP para fecha: {}", today);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            if (response.getBody() == null) {
                log.warn("Respuesta vacía de la API");
                return;
            }

            // La API devuelve { "success": 1, "result": [...] }
            Object resultObj = response.getBody().get("result");
            if (!(resultObj instanceof List)) {
                log.warn("Formato inesperado en respuesta de la API: {}", response.getBody());
                return;
            }

            List<Map<String, Object>> apiMatches = (List<Map<String, Object>>) resultObj;
            List<Match> dbMatches = matchRepo.findByMatchDateOrderByMatchTimeAsc(LocalDate.now());

            log.info("Partidos en API: {} | Partidos en DB hoy: {}", apiMatches.size(), dbMatches.size());

            for (Map<String, Object> apiMatch : apiMatches) {
                String status = String.valueOf(apiMatch.get("event_status"));
                if (!"Finished".equalsIgnoreCase(status)) continue;

                String p1     = String.valueOf(apiMatch.get("event_first_player"));
                String p2     = String.valueOf(apiMatch.get("event_second_player"));
                String winner = String.valueOf(apiMatch.get("event_winner")); // "First Player" o "Second Player"

                dbMatches.stream()
                    .filter(m -> nombresCoinciden(m.getPlayer1(), m.getPlayer2(), p1, p2))
                    .findFirst()
                    .ifPresent(m -> {
                        MatchResultDto dto = buildResult(m, apiMatch, winner);
                        if (dto != null) {
                            matchAdminService.saveResult(m.getId(), dto);
                            log.info("✓ Resultado guardado: {} vs {} → {}",
                                m.getPlayer1(), m.getPlayer2(), dto.getWinner());
                        }
                    });
            }

        } catch (Exception e) {
            log.error("Error al sincronizar con API de tenis: {}", e.getMessage(), e);
        }
    }

    private MatchResultDto buildResult(Match match, Map<String, Object> apiMatch, String winnerField) {
        try {
            // event_final_result: "2 - 0" o "2 - 1"
            String finalResult = String.valueOf(apiMatch.get("event_final_result"));
            int setsWinner = 0;

            if (finalResult != null && finalResult.contains(" - ")) {
                String[] parts = finalResult.split(" - ");
                setsWinner = Integer.parseInt(parts[0].trim());
            }

            // event_game_result: "6-3, 6-4" o "6-3, 4-6, 6-2"
            String gameResult = String.valueOf(apiMatch.get("event_game_result"));
            int totalGamesWinner = 0, totalGamesLoser = 0;

            if (gameResult != null && !gameResult.equals("null") && !gameResult.equals("-")) {
                String[] sets = gameResult.split(",");
                for (String set : sets) {
                    set = set.trim().replaceAll("\\(.*\\)", "").trim();
                    if (set.contains("-")) {
                        String[] games = set.split("-");
                        if (games.length == 2) {
                            try {
                                int g1 = Integer.parseInt(games[0].trim());
                                int g2 = Integer.parseInt(games[1].trim());
                                totalGamesWinner += g1;
                                totalGamesLoser  += g2;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }

            // "First Player" gana → player1 de nuestra DB es el ganador
            boolean firstPlayerWon = "First Player".equalsIgnoreCase(winnerField);
            String winnerName = firstPlayerWon ? match.getPlayer1() : match.getPlayer2();

            // Si ganó el segundo jugador, invertir los games
            if (!firstPlayerWon) {
                int tmp = totalGamesWinner;
                totalGamesWinner = totalGamesLoser;
                totalGamesLoser  = tmp;
            }

            return MatchResultDto.builder()
                .winner(winnerName)
                .setsWinner(setsWinner > 0 ? setsWinner : null)
                .gamesWinner(totalGamesWinner > 0 ? totalGamesWinner : null)
                .gamesLoser(totalGamesLoser  > 0 ? totalGamesLoser  : null)
                .build();

        } catch (Exception e) {
            log.error("Error parseando resultado del partido {}: {}", match.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Compara por apellido para tolerar diferencias de formato.
     * Ej: "Carlos Alcaraz" vs "C. Alcaraz" → true
     */
    private boolean nombresCoinciden(String dbP1, String dbP2, String apiP1, String apiP2) {
        String last1db  = apellido(dbP1);
        String last2db  = apellido(dbP2);
        String last1api = apellido(apiP1);
        String last2api = apellido(apiP2);

        return (last1db.equalsIgnoreCase(last1api) && last2db.equalsIgnoreCase(last2api))
            || (last1db.equalsIgnoreCase(last2api) && last2db.equalsIgnoreCase(last1api));
    }

    private String apellido(String nombre) {
        if (nombre == null || nombre.isBlank()) return "";
        String[] partes = nombre.trim().split("\\s+");
        return partes[partes.length - 1].toLowerCase();
    }
}
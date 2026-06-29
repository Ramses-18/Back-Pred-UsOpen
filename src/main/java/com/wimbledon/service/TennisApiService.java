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


    @Scheduled(fixedDelay = 600_000)
public void syncTodayResults() {
    String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String url = "https://" + apiHost
        + "/tennis/v2/atp/fixtures/" + today
        + "?filter=PlayerGroup:singles&pageSize=100";

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-RapidAPI-Key",  apiKey);
    headers.set("X-RapidAPI-Host", apiHost);

    log.info("Sincronizando resultados ATP - {}", today);

    try {
        ResponseEntity<Map> response = restTemplate.exchange(
            url, HttpMethod.GET,
            new HttpEntity<>(headers), Map.class
        );

        if (response.getBody() == null) return;

        // La API devuelve: { "data": [...], "hasNextPage": bool }
        Object dataObj = response.getBody().get("data");
        if (!(dataObj instanceof List)) {
            log.warn("Formato inesperado, 'data' no es una lista: {}", dataObj);
            return;
        }

        List<Map<String, Object>> fixtures = (List<Map<String, Object>>) dataObj;
        List<Match> dbMatches = matchRepo.findByMatchDateOrderByMatchTimeAsc(LocalDate.now());

        log.info("Fixtures API: {} | Partidos en DB hoy: {}", fixtures.size(), dbMatches.size());

        for (Map<String, Object> fixture : fixtures) {
            String result = (String) fixture.get("result");
            if (result == null || result.isBlank()) continue;

            Map<String, Object> p1 = (Map<String, Object>) fixture.get("player1");
            Map<String, Object> p2 = (Map<String, Object>) fixture.get("player2");
            if (p1 == null || p2 == null) continue;

            String winnerName = (String) p1.get("name");
            String loserName  = (String) p2.get("name");

            dbMatches.stream()
                .filter(m -> coincidePartido(m, winnerName, loserName))
                .findFirst()
                .ifPresent(m -> {
                    MatchResultDto dto = parsearResultado(m, winnerName, result);
                    if (dto != null) {
                        matchAdminService.saveResult(m.getId(), dto);
                        log.info("✓ Resultado: {} derrotó a {} ({})",
                            winnerName, loserName, result);
                    }
                });
        }

    } catch (Exception e) {
        log.error("Error sincronizando: {}", e.getMessage(), e);
    }
}

    /**
     * Parsea el resultado "6-3 6-4" o "6-3 4-6 6-2"
     * player1 de la API = ganador
     */
    private MatchResultDto parsearResultado(Match match, String winnerName, String result) {
        try {
            String[] sets = result.trim().split("\\s+");
            int setsGanados = sets.length; // 2 o 3
            int gamesWinner = 0, gamesLoser = 0;

            for (String set : sets) {
                // Puede venir "6-3" o "7-6(4)"
                String setLimpio = set.replaceAll("\\(.*\\)", "").trim();
                if (setLimpio.contains("-")) {
                    String[] games = setLimpio.split("-");
                    gamesWinner += Integer.parseInt(games[0].trim());
                    gamesLoser  += Integer.parseInt(games[1].trim());
                }
            }

            // Determinar nombre correcto del ganador en nuestra DB
            String ganadorEnDB = apellido(winnerName).equalsIgnoreCase(apellido(match.getPlayer1()))
                ? match.getPlayer1() : match.getPlayer2();

            return MatchResultDto.builder()
                .winner(ganadorEnDB)
                .setsWinner(setsGanados)
                .gamesWinner(gamesWinner > 0 ? gamesWinner : null)
                .gamesLoser(gamesLoser  > 0 ? gamesLoser  : null)
                .build();

        } catch (Exception e) {
            log.error("Error parseando '{}': {}", result, e.getMessage());
            return null;
        }
    }

    private boolean coincidePartido(Match m, String apiWinner, String apiLoser) {
        String lastW  = apellido(apiWinner);
        String lastL  = apellido(apiLoser);
        String lastP1 = apellido(m.getPlayer1());
        String lastP2 = apellido(m.getPlayer2());
        return (lastP1.equalsIgnoreCase(lastW) && lastP2.equalsIgnoreCase(lastL))
            || (lastP1.equalsIgnoreCase(lastL) && lastP2.equalsIgnoreCase(lastW));
    }

    private String apellido(String nombre) {
        if (nombre == null || nombre.isBlank()) return "";
        String[] partes = nombre.trim().split("\\s+");
        return partes[partes.length - 1].toLowerCase();
    }
}
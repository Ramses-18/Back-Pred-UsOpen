package com.wimbledon.service;

import com.wimbledon.dto.MatchResultDto;
import com.wimbledon.repository.MatchRepository;
import com.wimbledon.entity.Match;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
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

    private final MatchRepository       matchRepo;
    private final MatchAdminService     matchAdminService;
    private final RestTemplate          restTemplate;

    // Se ejecuta cada 10 minutos durante el torneo
    @Scheduled(fixedDelay = 600_000)
    public void syncResults() {
        String url = "https://" + apiHost +
                     "/tennis/v2/atp/tournament/540/results?season=2026";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key",  apiKey);
        headers.set("X-RapidAPI-Host", apiHost);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(headers), Map.class
            );

            List<Map<String, Object>> matches =
                (List<Map<String, Object>>) response.getBody().get("data");

            if (matches == null) return;

            LocalDate today = LocalDate.now();

            for (Map<String, Object> apiMatch : matches) {
                String status = (String) apiMatch.get("status");
                if (!"Finished".equals(status)) continue;

                // Buscar el partido en nuestra DB por jugadores y fecha
                String p1 = getPlayerName(apiMatch, "homePlayer");
                String p2 = getPlayerName(apiMatch, "awayPlayer");

                matchRepo.findByMatchDateOrderByMatchTimeAsc(today)
                    .stream()
                    .filter(m -> namesMatch(m, p1, p2))
                    .findFirst()
                    .ifPresent(m -> saveResult(m, apiMatch));
            }

        } catch (Exception e) {
            log.error("Error sincronizando resultados: {}", e.getMessage());
        }
    }

    private void saveResult(Match match, Map<String, Object> apiMatch) {
        try {
            Map<String, Object> score = (Map<String, Object>) apiMatch.get("score");
            if (score == null) return;

            String winner = getPlayerName(apiMatch,
                "1".equals(apiMatch.get("winnerId").toString().equals(
                    getPlayerId(apiMatch, "homePlayer").toString())
                    ? "homePlayer" : "awayPlayer")
                    ? "homePlayer" : "awayPlayer");

            // Sets ganados
            List<Map<String,Object>> sets =
                (List<Map<String,Object>>) score.get("sets");

            int setsHome = 0, setsAway = 0, gamesHome = 0, gamesAway = 0;
            if (sets != null) {
                for (Map<String, Object> set : sets) {
                    int h = toInt(set.get("homeScore"));
                    int a = toInt(set.get("awayScore"));
                    gamesHome += h;
                    gamesAway += a;
                    if (h > a) setsHome++; else setsAway++;
                }
            }

            boolean homeWon = setsHome > setsAway;
            MatchResultDto dto = MatchResultDto.builder()
                .winner(homeWon ? match.getPlayer1() : match.getPlayer2())
                .setsWinner(homeWon ? setsHome : setsAway)
                .gamesWinner(homeWon ? gamesHome : gamesAway)
                .gamesLoser(homeWon ? gamesAway : gamesHome)
                .build();

            matchAdminService.saveResult(match.getId(), dto);
            log.info("Resultado sincronizado: {} vs {}", match.getPlayer1(), match.getPlayer2());

        } catch (Exception e) {
            log.error("Error guardando resultado para partido {}: {}", match.getId(), e.getMessage());
        }
    }

    private String getPlayerName(Map<String, Object> m, String key) {
        Map<String, Object> player = (Map<String, Object>) m.get(key);
        return player != null ? (String) player.get("name") : "";
    }

    private Object getPlayerId(Map<String, Object> m, String key) {
        Map<String, Object> player = (Map<String, Object>) m.get(key);
        return player != null ? player.get("id") : null;
    }

    private boolean namesMatch(Match m, String p1, String p2) {
        String mp1 = m.getPlayer1().toLowerCase();
        String mp2 = m.getPlayer2().toLowerCase();
        String ap1 = p1.toLowerCase();
        String ap2 = p2.toLowerCase();
        return (mp1.contains(ap1.split(" ")[0]) || ap1.contains(mp1.split(" ")[0]))
            && (mp2.contains(ap2.split(" ")[0]) || ap2.contains(mp2.split(" ")[0]));
    }

    private int toInt(Object o) {
        if (o == null) return 0;
        return Integer.parseInt(o.toString());
    }
}
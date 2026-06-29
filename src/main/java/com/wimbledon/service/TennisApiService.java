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
import java.time.LocalDateTime;
import java.time.LocalTime;
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

            // Ignorar partidos de dobles
            if (player1Name.contains("/") || player2Name.contains("/")) continue;

            // Obtener cancha y ronda si vienen
            String court = "TBD";
            String round = null;
            // Filtrar solo Wimbledon
            Map<String, Object> tournament = (Map<String, Object>) fixture.get("tournament");
            if (tournament != null) {
                String tournamentName = String.valueOf(tournament.get("name"));
            if (!tournamentName.toLowerCase().contains("wimbledon")) continue;
                court = tournamentName;
            }
            
            Map<String, Object> roundObj = (Map<String, Object>) fixture.get("round");
            if (roundObj != null) {
                Object roundName = roundObj.get("name");
                if (roundName != null) round = roundName.toString();
            }

            // Obtener hora del partido
            LocalTime matchTime = LocalTime.of(12, 0); // default
            Object dateObj = fixture.get("date");
            if (dateObj != null) {
                try {
                    LocalDateTime dt = LocalDateTime.parse(
                        dateObj.toString().replace("Z", ""),
                        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    );
                    matchTime = dt.toLocalTime();
                } catch (Exception ignored) {}
            }

            final String p1Final = player1Name;
            final String p2Final = player2Name;
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
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j

public class TennisApiService {

    @Value("${app.tennis-api.key}")
    private String apiKey;
    @Value("${app.tennis-api.host}")
    private String apiHost;
    @Value("${app.tennis-api.base-url:https://api-sports.io/tennis}")
    private String baseUrl; // https://api-sports.io/tennis

    private final MatchRepository matchRepo;
    private final MatchAdminService matchAdminService;
    private final CourtQueueService courtQueueService;
    private final RestTemplate restTemplate;

    @Scheduled(cron = "0 0 6 * * *", zone = "Europe/London")
    public void syncDailySchedule() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));
        String url = baseUrl + "/fixtures?date=" + today + "&league=84"; // 84 = Wimbledon

        HttpHeaders headers = apiHeaders();
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            List<Map<String, Object>> fixtures = (List) resp.getBody().get("response");

            // Agrupar por venue (cancha)
            Map<String, List<Map<String, Object>>> byCourt = fixtures.stream()
                    .collect(Collectors
                            .groupingBy(f -> ((Map) ((Map) f.get("fixture")).get("venue")).get("name").toString()));

            // Para cada cancha, ordenar por timestamp y crear matches con orderInCourt y
            // followsMatchId
            for (var entry : byCourt.entrySet()) {
                String court = entry.getKey();
                List<Map<String, Object>> courtMatches = entry.getValue().stream()
                        .sorted(Comparator
                                .comparing(f -> Long.valueOf(((Map) f.get("fixture")).get("timestamp").toString())))
                        .collect(Collectors.toList());

                Long previousId = null;
                int order = 1;
                for (Map<String, Object> fx : courtMatches) {
                    String apiEventId = String.valueOf(((Map) fx.get("fixture")).get("id"));
                    // Upsert: si ya existe por apiEventId, update; si no, create
                    Match m = matchRepo.findByApiEventId(apiEventId)
                            .orElseGet(() -> new Match());

                    Map fixture = (Map) fx.get("fixture");
                    Map players = (Map) fx.get("players"); // [{player:{name}}, {player:{name}}]

                    m.setMatchDate(today);
                    m.setMatchTime(timestampToLocalTime(fixture.get("timestamp")));
                    m.setCourt(court);
                    m.setPlayer1(playerName(players, 0));
                    m.setPlayer2(playerName(players, 1));
                    m.setRound(((Map) fx.get("league")).get("round").toString());
                    m.setOrderInCourt(order);
                    m.setFollowsMatchId(previousId);
                    m.setApiEventId(apiEventId);
                    if (m.getStatus() == null)
                        m.setStatus("SCHEDULED");

                    matchRepo.save(m);
                    previousId = m.getId();
                    order++;
                }
            }
            log.info("✓ Schedule del día sincronizado: {} partidos en {} canchas",
                    fixtures.size(), byCourt.size());
        } catch (Exception e) {
            log.error("Error en syncDailySchedule: {}", e.getMessage(), e);
        }
    }

    // Cada 2 min — actualiza status y scores de los partidos en vivo
    @Scheduled(fixedDelay = 120_000)
    public void syncLiveStatus() {
        List<Match> scheduledOrLive = matchRepo.findByMatchDateAndStatusIn(
                LocalDate.now(ZoneId.of("Europe/London")),
                List.of("SCHEDULED", "IN_PLAY", "SUSPENDED"));

        if (scheduledOrLive.isEmpty())
            return;

        for (Match m : scheduledOrLive) {
            if (m.getApiEventId() == null)
                continue;
            try {
                String url = baseUrl + "/fixtures?id=" + m.getApiEventId();
                ResponseEntity<Map> resp = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(apiHeaders()), Map.class);
                List<Map<String, Object>> results = (List) resp.getBody().get("response");
                if (results.isEmpty())
                    continue;

                Map fixture = (Map) ((Map) results.get(0)).get("fixture");
                String status = ((Map) fixture.get("status")).get("code").toString(); // NS, IN, FIN, ...

                // Transición SCHEDULED -> IN_PLAY: registrar actualStartTime
                if ("IN".equals(status) && "SCHEDULED".equals(m.getStatus())) {
                    m.setActualStartTime(LocalDateTime.now());
                    m.setStatus("IN_PLAY");
                    matchRepo.save(m);
                    log.info("🎾 Arrancó: {} vs {} en {}", m.getPlayer1(), m.getPlayer2(), m.getCourt());
                }

                // Transición a FINISHED: guardar resultado + propagar a siguientes
                if (("FIN".equals(status) || "WO".equals(status) || "ABD".equals(status))
                        && !"FINISHED".equals(m.getStatus())) {
                    m.setActualEndTime(LocalDateTime.now());
                    m.setStatus("FINISHED");
                    matchRepo.save(m);

                    // Extraer score y indicator del fixture de API-Sports
                    Map scoresMap = (Map) results.get(0).get("scores"); // {"set1": {"player1": 6, "player2": 4}, ...}
                    String score = buildScoreString(scoresMap); // "6-4,3-6,7-5"
                    String indicator = buildIndicator(scoresMap); // "1,0" si ganó p1, "0,1" si ganó p2
                    String p1 = m.getPlayer1();
                    String p2 = m.getPlayer2();

                    MatchResultDto dto = parsearResultadoLive(m, p1, p2, score, indicator);
                    if (dto != null) {
                        matchAdminService.saveResult(m.getId(), dto);
                        log.info("✓ Resultado final: {} vs {} | {} | ganador: {}", p1, p2, score, dto.getWinner());
                    }

                    courtQueueService.recalcularEstimadosEnCancha(m.getCourt(), m.getMatchDate());
                }
            } catch (Exception e) {
                log.error("Error sync match {}: {}", m.getApiEventId(), e.getMessage());
            }
        }
    }

    private String buildScoreString(Map scores) {
        if (scores == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            Object setObj = scores.get("set" + i);
            if (!(setObj instanceof Map))
                break;
            Map s = (Map) setObj;
            Object a = s.get("player1");
            Object b = s.get("player2");
            if (a == null || b == null)
                break;
            if (sb.length() > 0)
                sb.append(",");
            sb.append(a).append("-").append(b);
        }
        return sb.toString();
    }

    private String buildIndicator(Map scores) {
        if (scores == null)
            return "1,0";
        int p1Sets = 0, p2Sets = 0;
        for (int i = 1; i <= 5; i++) {
            Object setObj = scores.get("set" + i);
            if (!(setObj instanceof Map))
                break;
            Map s = (Map) setObj;
            Object a = s.get("player1");
            Object b = s.get("player2");
            if (a == null || b == null)
                break;
            try {
                int ga = Integer.parseInt(a.toString());
                int gb = Integer.parseInt(b.toString());
                if (ga > gb)
                    p1Sets++;
                else if (gb > ga)
                    p2Sets++;
            } catch (NumberFormatException ignored) {
            }
        }
        return p1Sets >= p2Sets ? "1,0" : "0,1";
    }

    private void recalcularEstimadosEnCancha(String court, LocalDate date) {
        List<Match> cola = matchRepo.findByMatchDateAndCourtOrderByOrderInCourtAsc(date, court);
        for (Match m : cola) {
            if ("SCHEDULED".equals(m.getStatus()) && m.getFollowsMatchId() != null) {
                // Si el padre terminó, este puede arrancar ahora → estimated = ahora + 10 min
                Match padre = matchRepo.findById(m.getFollowsMatchId()).orElse(null);
                if (padre != null && padre.getActualEndTime() != null) {
                    // El front mostrará "A continuación" en vez de hora
                    log.debug("Partido {} listo para arrancar en {}", m.getId(), court);
                }
            }
        }
    }

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

    private HttpHeaders apiHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-RapidAPI-Key", apiKey);
        h.set("X-RapidAPI-Host", apiHost);
        return h;
    }

    private LocalTime timestampToLocalTime(Object ts) {
        if (ts == null)
            return null;
        try {
            long epoch = Long.parseLong(ts.toString());
            return Instant.ofEpochSecond(epoch)
                    .atZone(ZoneId.of("Europe/London"))
                    .toLocalTime();
        } catch (Exception e) {
            return null;
        }
    }

    private String playerName(Map players, int idx) {
        try {
            return ((Map) ((List) players).get(idx)).get("name").toString();
        } catch (Exception e) {
            return "TBD";
        }
    }
}
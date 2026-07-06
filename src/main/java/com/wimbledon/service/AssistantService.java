package com.wimbledon.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AssistantService {

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            Eres el asistente oficial de Wimbledon 2026 Predictions.
            Respondé SIEMPRE en español (argentina).
            Basate UNICAMENTE en el reglamento y la información que se te proporciona a continuación.
            Si la pregunta no está relacionada con el torneo, los puntos, las reglas o el funcionamiento de la app,
            respondé amablemente que solo podés ayudar con temas del torneo Wimbledon 2026 Predictions.
            Seas conciso pero completo. Usá un tono amigable y deportivo.

            === REGLAMENTO DE WIMBLEDON 2026 PREDICTIONS ===

            PUNTUACIÓN:
            - Ganador correcto: +1 punto
            - Sets correctos (quién ganó cada set, sin importar games): +3 puntos
            - Resultado exacto set a set (incluye cantidad de games): +10 puntos
            - Si el partido termina por retiro (RET): solo se otorgan puntos por ganador correcto (+1), no por sets.

            CORRECCIÓN DIARIA:
            - Cada usuario tiene UNA corrección diaria.
            - Puede usarse para cambiar su pick de UN partido que aún no empezó (status SCHEDULED).
            - Una vez que el admin cierra el pronóstico (deadline) o el partido pasa a IN_PLAY, no se puede corregir.
            - La corrección se reinicia cada día (medianoche Buenos Aires).

            PICKS:
            - Los usuarios hacen su pronóstico antes de que empiece cada partido.
            - Elegís: ganador, sets ganador, sets perdedor, y opcionalmente el score exacto set a set.
            - Si no elegís score exacto, solo se puntúa por ganador y/o sets.
            - Una vez cerrado el pronóstico, no se puede editar (salvo con la corrección diaria).

            TORNEO (PRONÓSTICO DEL TORNEO):
            - Además de los picks diarios, cada usuario elige: CAMPEÓN y 4 SEMIFINALISTAS.
            - Se puntúa cuando el torneo finaliza.
            - Puntos por campeón correcto y semifinalistas correctos.

            STATUS DE PARTIDOS:
            - SCHEDULED: programado, se puede hacer pick.
            - IN_PLAY: en juego, no se puede hacer pick.
            - SUSPENDED: suspendido temporalmente.
            - FINISHED: terminado, ya hay resultado.
            - WALKOVER / RETIRED: un jugador no se presentó o se retiró.
            - ABANDONED: partido abandonado.

            NOTIFICACIONES PUSH:
            - Podés activar notificaciones push para saber cuándo empieza un partido de tu pick
              y cuando se cargan resultados.
            - Se activan desde el onboarding o la configuración.

            TABLA DE POSICIONES:
            - Muestra el ranking de todos los usuarios por puntos totales.
            - Se actualiza en tiempo real cuando se cargan resultados.

            CUADRO (BRACKET):
            - Muestra el draw del torneo desde cuartos de final (R16).
            - Se actualiza manualmente por el admin cuando avanzan los jugadores.

            ATP RANKING:
            - Tabla con los 100 mejores jugadores del ranking ATP.
            - Se muestra en la pestaña Ranking.
            === FIN DEL REGLAMENTO ===
            """;

    private static final List<String> FAQ_ENTRIES = List.of();

    public String answer(String question) {
        log.info("[Assistant] pregunta: {}", question.length() > 80 ? question.substring(0, 80) + "..." : question);

        try {
            // Buscar FAQ rápido primero (simulado por el system prompt ya)
            // Si hay API key, usar LLM
            if (openaiApiKey != null && !openaiApiKey.isBlank()) {
                return callLLM(question);
            }

            // Sin API key: usar matching por keywords como fallback
            return fallbackAnswer(question);
        } catch (Exception e) {
            log.error("[Assistant] error: {}", e.getMessage(), e);
            return "Disculpá, tuve un problema para procesar tu pregunta. Intentá de nuevo en un momento.";
        }
    }

    private String callLLM(String question) throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": %s},
                    {"role": "user", "content": %s}
                  ],
                  "max_tokens": 500,
                  "temperature": 0.7
                }
                """.formatted(model, escapeJson(SYSTEM_PROMPT), escapeJson(question));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(openaiApiUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + openaiApiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("[Assistant] LLM response status: {}, body: {}", response.statusCode(), response.body());
            return fallbackAnswer(question);
        }

        // Parsear respuesta JSON simple
        return parseLLMResponse(response.body());
    }

    private String parseLLMResponse(String json) {
        // Buscar "content": "..." en la respuesta
        Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher matcher = pattern.matcher(json);

        // El primer content es del system message, el segundo es la respuesta
        String lastContent = null;
        while (matcher.find()) {
            lastContent = matcher.group(1);
        }

        if (lastContent != null) {
            return lastContent
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
        }

        return "No pude generar una respuesta. Intentá de nuevo.";
    }

    private String fallbackAnswer(String q) {
        String lower = q.toLowerCase();

        if (lower.contains("punto") || lower.contains("score") || lower.contains("cuánto vale") || lower.contains("cuanto vale")) {
            return "La puntuación es:\n\n• Ganador correcto: **+1 punto**\n• Sets correctos: **+3 puntos**\n• Resultado exacto set a set: **+10 puntos**\n• Retiro: solo +1 por ganador correcto.";
        }
        if (lower.contains("corrección") || lower.contains("corregir") || lower.contains("cambiar pick") || lower.contains("editar pick")) {
            return "Tenés **UNA corrección diaria** que te permite cambiar tu pick de un partido que aún no empezó (SCHEDULED). Se reinicia cada medianoche (Buenos Aires). Una vez que el admin cierra el pronóstico, no se puede usar.";
        }
        if (lower.contains("pick") || lower.contains("pronóstico") || lower.contains("pronostico") || lower.contains("predecir")) {
            return "Para hacer tu pick:\n\n1. Entrá a la pestaña **Hoy**\n2. Elegí el ganador de cada partido\n3. Opcionalmente cargá sets y score exacto\n4. ¡Listo! Una vez cerrado el pronóstico no se puede editar (salvo con la corrección diaria).";
        }
        if (lower.contains("campeón") || lower.contains("campeon") || lower.contains("semifinal") || lower.contains("torneo")) {
            return "En la pestaña **Torneo** podés elegir tu **campeón** y tus **4 semifinalistas**. Se puntúa cuando el torneo finaliza. ¡Elegí bien!";
        }
        if (lower.contains("tabla") || lower.contains("posición") || lower.contains("posicion") || lower.contains("ranking") || lower.contains("clasificación")) {
            return "La **tabla de posiciones** muestra el ranking de todos los usuarios por puntos totales. Se actualiza en tiempo real cuando se cargan resultados.";
        }
        if (lower.contains("notificación") || lower.contains("notificacion") || lower.contains("alerta")) {
            return "Podés activar **notificaciones push** para saber cuándo empieza un partido de tu pick y cuando se cargan resultados. Se configuran desde el onboarding al iniciar sesión.";
        }
        if (lower.contains("cuadro") || lower.contains("bracket") || lower.contains("draw") || lower.contains("llave")) {
            return "El **cuadro** muestra el draw del torneo desde cuartos de final (R16). Se actualiza manualmente cuando avanzan los jugadores. Lo ves en la pestaña **Cuadro**.";
        }
        if (lower.contains("retiro") || lower.contains("retir") || lower.contains("walkover") || lower.contains("abandon")) {
            return "Si un partido termina por retiro, solo se otorgan **+1 punto** por ganador correcto. No se puntúan sets ni score exacto.";
        }
        if (lower.contains("hola") || lower.contains("buenas") || lower.contains("hey") || lower.contains("hi")) {
            return "¡Buenas! 🎾 Soy el asistente de Wimbledon 2026 Predictions. Preguntame sobre reglas, puntos, picks, correcciones o lo que necesites.";
        }
        if (lower.contains("admin") || lower.contains("cerrar") || lower.contains("deadline")) {
            return "El **admin** puede cerrar pronósticos, cambiar estados de partidos (IN_PLAY, SUSPENDED, FINISHED), cargar resultados y gestionar el cuadro. Una vez cerrado el pronóstico, los usuarios no pueden editar sus picks.";
        }

        return "Preguntame sobre: **puntos**, **correcciones**, **picks**, **pronóstico del torneo**, **tabla**, **notificaciones**, **cuadro** o **reglas**. ¡Estoy para ayudar! 🎾";
    }

    private String escapeJson(String s) {
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
            .replace("\t", "\\t");
    }
}
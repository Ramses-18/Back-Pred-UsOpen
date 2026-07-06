package com.wimbledon.controller;

import com.wimbledon.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String question = body.get("message");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mensaje vacío"));
        }
        String answer = assistantService.answer(question.trim());
        return ResponseEntity.ok(Map.of("reply", answer));
    }
}
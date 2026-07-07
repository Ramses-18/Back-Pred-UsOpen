package com.wimbledon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> handleBadArg(IllegalArgumentException ex) {
        log.warn("[400] IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,String>> handleState(IllegalStateException ex) {
        log.warn("[409] IllegalStateException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleAll(Exception ex) {
        log.error("[500] UNHANDLED EXCEPTION — type: {} | message: {}",
            ex.getClass().getSimpleName(), ex.getMessage(), ex);
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("error", "Error interno: " + ex.getClass().getSimpleName() + " — " + ex.getMessage());
        body.put("exceptionType", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
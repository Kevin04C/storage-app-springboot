package com.stonestorage.bootstrap.controller;

import com.stonestorage.shared.infrastructure.web.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("application", "StoneStorage");
        response.put("version", "0.0.1-SNAPSHOT");

        return ApiResponse.ok(response, "Service is healthy");
    }

    @GetMapping("/ready")
    public ApiResponse<Map<String, Object>> ready() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "READY");
        response.put("timestamp", LocalDateTime.now().toString());

        return ApiResponse.ok(response, "Service is ready");
    }
}

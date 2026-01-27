package com.horizon.backend.controller;

import com.horizon.backend.common.ApiResponse;
import com.horizon.backend.dto.metrics.MetricsDto;
import com.horizon.backend.dto.metrics.MetricsRequest;
import com.horizon.backend.dto.metrics.RealtimeMetrics;
import com.horizon.backend.service.MetricsService;
import com.horizon.backend.service.SseEmitterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;
    private final SseEmitterService sseEmitterService;

    @PostMapping("/agents/{agentId}")
    public ResponseEntity<ApiResponse<MetricsDto>> receiveMetrics(
            @PathVariable Long agentId,
            @Valid @RequestBody MetricsRequest request) {
        MetricsDto metrics = metricsService.saveMetrics(agentId, request);

        RealtimeMetrics realtime = metricsService.getRealtimeMetrics(agentId);
        sseEmitterService.sendToAll(realtime);

        return ResponseEntity.ok(ApiResponse.success(metrics, "Metrics received successfully"));
    }

    @GetMapping("/realtime/{agentId}")
    public ResponseEntity<ApiResponse<RealtimeMetrics>> getRealtimeMetrics(@PathVariable Long agentId) {
        RealtimeMetrics metrics = metricsService.getRealtimeMetrics(agentId);
        return ResponseEntity.ok(ApiResponse.success(metrics, "Realtime metrics retrieved successfully"));
    }

    @GetMapping("/realtime")
    public ResponseEntity<ApiResponse<List<RealtimeMetrics>>> getAllRealtimeMetrics() {
        List<RealtimeMetrics> metrics = metricsService.getAllRealtimeMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics, "All realtime metrics retrieved successfully"));
    }

    @GetMapping("/history/{agentId}")
    public ResponseEntity<ApiResponse<List<MetricsDto>>> getMetricsHistory(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "100") int limit) {
        List<MetricsDto> history = metricsService.getMetricsHistory(agentId, limit);
        return ResponseEntity.ok(ApiResponse.success(history, "Metrics history retrieved successfully"));
    }

    @GetMapping("/history/{agentId}/range")
    public ResponseEntity<ApiResponse<List<MetricsDto>>> getMetricsHistoryRange(
            @PathVariable Long agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<MetricsDto> history = metricsService.getMetricsHistory(agentId, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(history, "Metrics history retrieved successfully"));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAllMetrics() {
        SseEmitter emitter = sseEmitterService.createEmitter();
        log.debug("New SSE connection for all agents metrics");

        try {
            List<RealtimeMetrics> initialMetrics = metricsService.getAllRealtimeMetrics();
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data(initialMetrics));
        } catch (Exception e) {
            log.error("Failed to send initial metrics", e);
        }

        return emitter;
    }

    @GetMapping(value = "/stream/{agentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAgentMetrics(@PathVariable Long agentId) {
        SseEmitter emitter = sseEmitterService.createEmitterForAgent(agentId);
        log.debug("New SSE connection for agent: {}", agentId);

        try {
            RealtimeMetrics initialMetrics = metricsService.getRealtimeMetrics(agentId);
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data(initialMetrics));
        } catch (Exception e) {
            log.error("Failed to send initial metrics for agent: {}", agentId, e);
        }

        return emitter;
    }
}

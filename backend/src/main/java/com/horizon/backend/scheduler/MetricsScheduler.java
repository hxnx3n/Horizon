package com.horizon.backend.scheduler;

import com.horizon.backend.service.MetricsService;
import com.horizon.backend.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsScheduler {

    private final SseEmitterService sseEmitterService;
    private final MetricsService metricsService;

    @Value("${metrics.retention-days:7}")
    private int retentionDays;

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        int connectionCount = sseEmitterService.getConnectionCount();
        if (connectionCount > 0) {
            sseEmitterService.sendHeartbeat();
            log.debug("Heartbeat sent to {} SSE connections", connectionCount);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldMetrics() {
        log.info("Starting scheduled cleanup of old metrics data (retention: {} days)", retentionDays);
        metricsService.deleteOldMetrics(retentionDays);
        log.info("Completed scheduled cleanup of old metrics data");
    }
}

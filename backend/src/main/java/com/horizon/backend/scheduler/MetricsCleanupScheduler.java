package com.horizon.backend.scheduler;

import com.horizon.backend.repository.AgentMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsCleanupScheduler {

    private final AgentMetricsRepository agentMetricsRepository;

    @Value("${horizon.metrics.retention-days:7}")
    private int retentionDays;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldMetrics() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        
        log.info("Starting metrics cleanup. Deleting metrics older than {} days (before {})", 
                retentionDays, cutoffDate);
        
        try {
            int deletedCount = agentMetricsRepository.deleteOlderThan(cutoffDate);
            log.info("Metrics cleanup completed. Deleted {} old metrics records", deletedCount);
        } catch (Exception e) {
            log.error("Failed to cleanup old metrics", e);
        }
    }
}

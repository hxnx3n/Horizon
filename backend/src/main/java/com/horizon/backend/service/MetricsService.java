package com.horizon.backend.service;

import com.horizon.backend.dto.metrics.MetricsDto;
import com.horizon.backend.dto.metrics.MetricsRequest;
import com.horizon.backend.dto.metrics.RealtimeMetrics;

import java.time.LocalDateTime;
import java.util.List;

public interface MetricsService {

    MetricsDto saveMetrics(Long agentId, MetricsRequest request);

    MetricsDto saveMetricsByIp(String agentIp, MetricsRequest request);

    RealtimeMetrics getRealtimeMetrics(Long agentId);

    List<RealtimeMetrics> getAllRealtimeMetrics();

    List<MetricsDto> getMetricsHistory(Long agentId, int limit);

    List<MetricsDto> getMetricsHistory(Long agentId, LocalDateTime startTime, LocalDateTime endTime);

    void deleteOldMetrics(int retentionDays);

    void deleteMetricsByAgentId(Long agentId);
}

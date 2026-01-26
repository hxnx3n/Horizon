package com.horizon.backend.dto.metrics;

import com.horizon.backend.entity.AgentMetrics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDto {

    private Long id;
    private Long agentId;
    private String agentName;
    private Double cpuUsage;
    private Long memoryTotal;
    private Long memoryUsed;
    private Double memoryUsage;
    private Long diskTotal;
    private Long diskUsed;
    private Double diskUsage;
    private Long networkRxBytes;
    private Long networkTxBytes;
    private Double loadAverage1m;
    private Double loadAverage5m;
    private Double loadAverage15m;
    private Integer processCount;
    private Long uptimeSeconds;
    private LocalDateTime createdAt;

    public static MetricsDto from(AgentMetrics metrics) {
        return MetricsDto.builder()
                .id(metrics.getId())
                .agentId(metrics.getAgent().getId())
                .agentName(metrics.getAgent().getName())
                .cpuUsage(metrics.getCpuUsage())
                .memoryTotal(metrics.getMemoryTotal())
                .memoryUsed(metrics.getMemoryUsed())
                .memoryUsage(metrics.getMemoryUsage())
                .diskTotal(metrics.getDiskTotal())
                .diskUsed(metrics.getDiskUsed())
                .diskUsage(metrics.getDiskUsage())
                .networkRxBytes(metrics.getNetworkRxBytes())
                .networkTxBytes(metrics.getNetworkTxBytes())
                .loadAverage1m(metrics.getLoadAverage1m())
                .loadAverage5m(metrics.getLoadAverage5m())
                .loadAverage15m(metrics.getLoadAverage15m())
                .processCount(metrics.getProcessCount())
                .uptimeSeconds(metrics.getUptimeSeconds())
                .createdAt(metrics.getCreatedAt())
                .build();
    }
}

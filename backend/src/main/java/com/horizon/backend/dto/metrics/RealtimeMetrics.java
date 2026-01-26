package com.horizon.backend.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeMetrics implements Serializable {

    private Long agentId;
    private String agentName;
    private String agentIp;
    private boolean online;

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

    private LocalDateTime timestamp;
    private LocalDateTime lastHeartbeat;

    public static RealtimeMetrics offline(Long agentId, String agentName, String agentIp) {
        return RealtimeMetrics.builder()
                .agentId(agentId)
                .agentName(agentName)
                .agentIp(agentIp)
                .online(false)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

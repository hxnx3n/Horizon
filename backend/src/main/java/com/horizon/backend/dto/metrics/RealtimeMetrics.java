package com.horizon.backend.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeMetrics implements Serializable {

    private Long agentId;
    private String agentName;
    private String hostname;
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
    private Double networkRxRate;
    private Double networkTxRate;
    private Double loadAverage1m;
    private Double loadAverage5m;
    private Double loadAverage15m;
    private Integer processCount;
    private Long uptimeSeconds;
    private Double temperature;

    private String nodeId;
    private String os;
    private String platform;

    private List<DiskInfo> disks;
    private List<NetworkInterfaceInfo> interfaces;

    private LocalDateTime timestamp;
    private LocalDateTime lastHeartbeat;

    public static RealtimeMetrics offline(Long agentId, String agentName, String hostname) {
        return RealtimeMetrics.builder()
                .agentId(agentId)
                .agentName(agentName)
                .hostname(hostname)
                .online(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskInfo implements Serializable {
        private String device;
        private String mountpoint;
        private Long totalBytes;
        private Long usedBytes;
        private Double usage;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NetworkInterfaceInfo implements Serializable {
        private String name;
        private List<String> ips;
        private Long sentBytes;
        private Long recvBytes;
        private Double sentRate;
        private Double recvRate;
    }
}

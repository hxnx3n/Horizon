package com.horizon.backend.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMetricsPayload {

    private String nodeId;
    private String os;
    private String platform;
    private Double cpuUsage;
    private Double temperature;
    private Double memUsage;
    private Long memoryTotal;
    private Long memoryUsed;
    private List<DiskMetric> disks;
    private List<InterfaceMetric> interfaces;
    private Long uptimeSeconds;
    private Long processCount;
    private String status;
    private String timestamp;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskMetric {
        private String device;
        private String mountpoint;
        private Long totalBytes;
        private Long usedBytes;
        private Double usage;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterfaceMetric {
        private String name;
        private List<String> ips;
        private Long sentBytes;
        private Long recvBytes;
        private Double sentRate;
        private Double recvRate;
    }
}

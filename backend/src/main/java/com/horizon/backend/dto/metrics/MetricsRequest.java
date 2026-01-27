package com.horizon.backend.dto.metrics;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsRequest {

    @NotNull(message = "CPU usage is required")
    @PositiveOrZero(message = "CPU usage must be zero or positive")
    private Double cpuUsage;

    @NotNull(message = "Memory total is required")
    @PositiveOrZero(message = "Memory total must be zero or positive")
    private Long memoryTotal;

    @NotNull(message = "Memory used is required")
    @PositiveOrZero(message = "Memory used must be zero or positive")
    private Long memoryUsed;

    @NotNull(message = "Disk total is required")
    @PositiveOrZero(message = "Disk total must be zero or positive")
    private Long diskTotal;

    @NotNull(message = "Disk used is required")
    @PositiveOrZero(message = "Disk used must be zero or positive")
    private Long diskUsed;

    @PositiveOrZero(message = "Network RX bytes must be zero or positive")
    private Long networkRxBytes;

    @PositiveOrZero(message = "Network TX bytes must be zero or positive")
    private Long networkTxBytes;

    private Double networkRxRate;

    private Double networkTxRate;

    @PositiveOrZero(message = "Load average must be zero or positive")
    private Double loadAverage1m;

    @PositiveOrZero(message = "Load average must be zero or positive")
    private Double loadAverage5m;

    @PositiveOrZero(message = "Load average must be zero or positive")
    private Double loadAverage15m;

    @PositiveOrZero(message = "Process count must be zero or positive")
    private Integer processCount;

    @PositiveOrZero(message = "Uptime must be zero or positive")
    private Long uptimeSeconds;

    private Double temperature;

    private String nodeId;

    private String os;

    private String platform;

    private List<DiskInfo> disks;

    private List<NetworkInterfaceInfo> interfaces;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskInfo {
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
    public static class NetworkInterfaceInfo {
        private String name;
        private List<String> ips;
        private Long sentBytes;
        private Long recvBytes;
        private Double sentRate;
        private Double recvRate;
    }
}

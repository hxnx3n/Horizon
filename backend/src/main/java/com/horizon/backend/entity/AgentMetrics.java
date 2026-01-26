package com.horizon.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_metrics", indexes = {
        @Index(name = "idx_agent_metrics_agent_id", columnList = "agent_id"),
        @Index(name = "idx_agent_metrics_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "cpu_usage", nullable = false)
    private Double cpuUsage;

    @Column(name = "memory_total", nullable = false)
    private Long memoryTotal;

    @Column(name = "memory_used", nullable = false)
    private Long memoryUsed;

    @Column(name = "memory_usage", nullable = false)
    private Double memoryUsage;

    @Column(name = "disk_total", nullable = false)
    private Long diskTotal;

    @Column(name = "disk_used", nullable = false)
    private Long diskUsed;

    @Column(name = "disk_usage", nullable = false)
    private Double diskUsage;

    @Column(name = "network_rx_bytes")
    private Long networkRxBytes;

    @Column(name = "network_tx_bytes")
    private Long networkTxBytes;

    @Column(name = "load_average_1m")
    private Double loadAverage1m;

    @Column(name = "load_average_5m")
    private Double loadAverage5m;

    @Column(name = "load_average_15m")
    private Double loadAverage15m;

    @Column(name = "process_count")
    private Integer processCount;

    @Column(name = "uptime_seconds")
    private Long uptimeSeconds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

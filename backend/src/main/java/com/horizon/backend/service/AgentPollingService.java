package com.horizon.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.horizon.backend.dto.metrics.MetricsRequest;
import com.horizon.backend.dto.metrics.RealtimeMetrics;
import com.horizon.backend.entity.Agent;
import com.horizon.backend.repository.AgentRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentPollingService {

    private final AgentRepository agentRepository;
    private final MetricsService metricsService;
    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper;

    @Value("${agent.polling.timeout:3000}")
    private long requestTimeout;

    private ScheduledExecutorService scheduler;
    private HttpClient httpClient;
    private final Map<Long, ScheduledFuture<?>> agentTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(requestTimeout))
                .build();

        scheduler = Executors.newScheduledThreadPool(10);
        scheduleAgentPolling();

        scheduler.scheduleAtFixedRate(this::scheduleAgentPolling, 30, 30, TimeUnit.SECONDS);
        log.info("Agent polling service started");
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("Agent polling service stopped");
    }

    private void scheduleAgentPolling() {
        try {
            List<Agent> agents = agentRepository.findByEnabled(true);

            for (Long agentId : agentTasks.keySet()) {
                boolean stillExists = agents.stream().anyMatch(a -> a.getId().equals(agentId));
                if (!stillExists) {
                    ScheduledFuture<?> task = agentTasks.remove(agentId);
                    if (task != null) {
                        task.cancel(false);
                    }
                }
            }

            for (Agent agent : agents) {
                ScheduledFuture<?> existingTask = agentTasks.get(agent.getId());

                if (existingTask == null || existingTask.isCancelled()) {
                    scheduleAgent(agent);
                }
            }
        } catch (Exception e) {
            log.error("Error scheduling agent polling", e);
        }
    }

    private void scheduleAgent(Agent agent) {
        long interval = agent.getPollingInterval() != null ? agent.getPollingInterval() : 1000L;

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                () -> pollAgent(agent.getId()),
                0,
                interval,
                TimeUnit.MILLISECONDS
        );

        agentTasks.put(agent.getId(), task);
        log.debug("Scheduled polling for agent: {} with interval: {}ms", agent.getName(), interval);
    }

    public void rescheduleAgent(Long agentId) {
        ScheduledFuture<?> existingTask = agentTasks.remove(agentId);
        if (existingTask != null) {
            existingTask.cancel(false);
        }

        agentRepository.findById(agentId).ifPresent(agent -> {
            if (agent.isEnabled()) {
                scheduleAgent(agent);
            }
        });
    }

    private void pollAgent(Long agentId) {
        try {
            Agent agent = agentRepository.findById(agentId).orElse(null);
            if (agent == null || !agent.isEnabled()) {
                return;
            }

            int port = agent.getPort() != null ? agent.getPort() : 9090;
            String url = String.format("http://%s:%d/metrics", agent.getIp(), port);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(requestTimeout))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                AgentMetricsResponse agentResponse = objectMapper.readValue(response.body(), AgentMetricsResponse.class);
                MetricsRequest metricsRequest = convertToMetricsRequest(agentResponse);
                metricsService.saveMetrics(agent.getId(), metricsRequest);

                RealtimeMetrics realtime = metricsService.getRealtimeMetrics(agent.getId());
                sseEmitterService.sendToAll(realtime);

                log.debug("Polled metrics from agent: {} ({}:{})", agent.getName(), agent.getIp(), port);
            } else {
                log.warn("Agent {} returned status: {}", agent.getName(), response.statusCode());
            }
        } catch (Exception e) {
            log.debug("Failed to poll agent {}: {}", agentId, e.getMessage());
        }
    }

    private MetricsRequest convertToMetricsRequest(AgentMetricsResponse response) {
        long diskTotal = 0;
        long diskUsed = 0;
        if (response.getDisks() != null) {
            for (DiskInfo disk : response.getDisks()) {
                diskTotal += disk.getTotalBytes();
                diskUsed += disk.getUsedBytes();
            }
        }

        long networkRx = 0;
        long networkTx = 0;
        if (response.getInterfaces() != null) {
            for (InterfaceInfo iface : response.getInterfaces()) {
                networkRx += iface.getRecvBytes();
                networkTx += iface.getSentBytes();
            }
        }

        return MetricsRequest.builder()
                .cpuUsage(response.getCpu())
                .memoryTotal(response.getMemTotalBytes())
                .memoryUsed(response.getMemUsedBytes())
                .diskTotal(diskTotal)
                .diskUsed(diskUsed)
                .networkRxBytes(networkRx)
                .networkTxBytes(networkTx)
                .build();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AgentMetricsResponse {
        private String nodeId;
        private String os;
        private String platform;
        private Double cpu;
        private Double temp;
        private Double memUsage;
        private Long memTotalBytes;
        private Long memUsedBytes;
        private List<DiskInfo> disks;
        private List<InterfaceInfo> interfaces;
        private String status;
        private String timestamp;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DiskInfo {
        private String device;
        private String mountpoint;
        private Long totalBytes;
        private Long usedBytes;
        private Double usage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InterfaceInfo {
        private String name;
        private List<String> ips;
        private Long sentBytes;
        private Long recvBytes;
        private Double sentRate;
        private Double recvRate;
    }
}

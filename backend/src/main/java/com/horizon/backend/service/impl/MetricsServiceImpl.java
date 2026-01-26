package com.horizon.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.horizon.backend.dto.metrics.MetricsDto;
import com.horizon.backend.dto.metrics.MetricsRequest;
import com.horizon.backend.dto.metrics.RealtimeMetrics;
import com.horizon.backend.entity.Agent;
import com.horizon.backend.entity.AgentMetrics;
import com.horizon.backend.exception.ResourceNotFoundException;
import com.horizon.backend.repository.AgentMetricsRepository;
import com.horizon.backend.repository.AgentRepository;
import com.horizon.backend.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements MetricsService {

    private final AgentRepository agentRepository;
    private final AgentMetricsRepository agentMetricsRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String METRICS_KEY_PREFIX = "agent:metrics:";
    private static final long METRICS_TTL_SECONDS = 60;

    @Override
    @Transactional
    public MetricsDto saveMetrics(Long agentId, MetricsRequest request) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", agentId));

        return saveMetricsInternal(agent, request);
    }

    @Override
    @Transactional
    public MetricsDto saveMetricsByIp(String agentIp, MetricsRequest request) {
        Agent agent = agentRepository.findByIp(agentIp)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "ip", agentIp));

        return saveMetricsInternal(agent, request);
    }

    private MetricsDto saveMetricsInternal(Agent agent, MetricsRequest request) {
        double memoryUsage = calculatePercentage(request.getMemoryUsed(), request.getMemoryTotal());
        double diskUsage = calculatePercentage(request.getDiskUsed(), request.getDiskTotal());

        AgentMetrics metrics = AgentMetrics.builder()
                .agent(agent)
                .cpuUsage(request.getCpuUsage())
                .memoryTotal(request.getMemoryTotal())
                .memoryUsed(request.getMemoryUsed())
                .memoryUsage(memoryUsage)
                .diskTotal(request.getDiskTotal())
                .diskUsed(request.getDiskUsed())
                .diskUsage(diskUsage)
                .networkRxBytes(request.getNetworkRxBytes())
                .networkTxBytes(request.getNetworkTxBytes())
                .loadAverage1m(request.getLoadAverage1m())
                .loadAverage5m(request.getLoadAverage5m())
                .loadAverage15m(request.getLoadAverage15m())
                .processCount(request.getProcessCount())
                .uptimeSeconds(request.getUptimeSeconds())
                .build();

        AgentMetrics savedMetrics = agentMetricsRepository.save(metrics);

        RealtimeMetrics realtimeMetrics = RealtimeMetrics.builder()
                .agentId(agent.getId())
                .agentName(agent.getName())
                .agentIp(agent.getIp())
                .online(true)
                .cpuUsage(request.getCpuUsage())
                .memoryTotal(request.getMemoryTotal())
                .memoryUsed(request.getMemoryUsed())
                .memoryUsage(memoryUsage)
                .diskTotal(request.getDiskTotal())
                .diskUsed(request.getDiskUsed())
                .diskUsage(diskUsage)
                .networkRxBytes(request.getNetworkRxBytes())
                .networkTxBytes(request.getNetworkTxBytes())
                .loadAverage1m(request.getLoadAverage1m())
                .loadAverage5m(request.getLoadAverage5m())
                .loadAverage15m(request.getLoadAverage15m())
                .processCount(request.getProcessCount())
                .uptimeSeconds(request.getUptimeSeconds())
                .timestamp(LocalDateTime.now())
                .lastHeartbeat(LocalDateTime.now())
                .build();

        saveToRedis(agent.getId(), realtimeMetrics);

        log.debug("Metrics saved for agent: {} ({})", agent.getName(), agent.getIp());

        return MetricsDto.from(savedMetrics);
    }

    @Override
    public RealtimeMetrics getRealtimeMetrics(Long agentId) {
        String key = METRICS_KEY_PREFIX + agentId;
        String json = redisTemplate.opsForValue().get(key);

        if (json != null) {
            try {
                return objectMapper.readValue(json, RealtimeMetrics.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize metrics from Redis for agent: {}", agentId, e);
            }
        }

        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", agentId));

        return RealtimeMetrics.offline(agent.getId(), agent.getName(), agent.getIp());
    }

    @Override
    public List<RealtimeMetrics> getAllRealtimeMetrics() {
        List<RealtimeMetrics> allMetrics = new ArrayList<>();
        List<Agent> agents = agentRepository.findByEnabled(true);

        for (Agent agent : agents) {
            String key = METRICS_KEY_PREFIX + agent.getId();
            String json = redisTemplate.opsForValue().get(key);

            if (json != null) {
                try {
                    RealtimeMetrics metrics = objectMapper.readValue(json, RealtimeMetrics.class);
                    LocalDateTime lastHeartbeat = metrics.getLastHeartbeat();
                    if (lastHeartbeat != null &&
                        Duration.between(lastHeartbeat, LocalDateTime.now()).getSeconds() > METRICS_TTL_SECONDS) {
                        metrics.setOnline(false);
                    }
                    allMetrics.add(metrics);
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize metrics from Redis for agent: {}", agent.getId(), e);
                    allMetrics.add(RealtimeMetrics.offline(agent.getId(), agent.getName(), agent.getIp()));
                }
            } else {
                allMetrics.add(RealtimeMetrics.offline(agent.getId(), agent.getName(), agent.getIp()));
            }
        }

        return allMetrics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MetricsDto> getMetricsHistory(Long agentId, int limit) {
        if (!agentRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("Agent", "id", agentId);
        }

        return agentMetricsRepository.findByAgentIdOrderByCreatedAtDesc(agentId, PageRequest.of(0, limit))
                .stream()
                .map(MetricsDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MetricsDto> getMetricsHistory(Long agentId, LocalDateTime startTime, LocalDateTime endTime) {
        if (!agentRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("Agent", "id", agentId);
        }

        return agentMetricsRepository.findByAgentIdAndCreatedAtBetweenOrderByCreatedAtAsc(agentId, startTime, endTime)
                .stream()
                .map(MetricsDto::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteOldMetrics(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        int deletedCount = agentMetricsRepository.deleteOlderThan(cutoffDate);
        log.info("Deleted {} old metrics records older than {} days", deletedCount, retentionDays);
    }

    @Override
    @Transactional
    public void deleteMetricsByAgentId(Long agentId) {
        int deletedCount = agentMetricsRepository.deleteByAgentId(agentId);
        String key = METRICS_KEY_PREFIX + agentId;
        redisTemplate.delete(key);
        log.info("Deleted {} metrics records for agent: {}", deletedCount, agentId);
    }

    private void saveToRedis(Long agentId, RealtimeMetrics metrics) {
        String key = METRICS_KEY_PREFIX + agentId;
        try {
            String json = objectMapper.writeValueAsString(metrics);
            redisTemplate.opsForValue().set(key, json, METRICS_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metrics to Redis for agent: {}", agentId, e);
        }
    }

    private double calculatePercentage(Long used, Long total) {
        if (total == null || total == 0) {
            return 0.0;
        }
        return Math.round((double) used / total * 10000) / 100.0;
    }
}

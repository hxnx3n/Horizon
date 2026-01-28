package com.horizon.backend.service.impl;

import com.horizon.backend.dto.agent.AgentMetricsPayload;
import com.horizon.backend.dto.agent.AgentPushRequest;
import com.horizon.backend.dto.agent.AgentRegisterRequest;
import com.horizon.backend.dto.agent.AgentRegisterResponse;
import com.horizon.backend.dto.metrics.MetricsRequest;
import com.horizon.backend.dto.metrics.RealtimeMetrics;
import com.horizon.backend.entity.Agent;
import com.horizon.backend.entity.ClientKey;
import com.horizon.backend.exception.BadRequestException;
import com.horizon.backend.repository.AgentRepository;
import com.horizon.backend.service.ClientKeyService;
import com.horizon.backend.service.AgentPushService;
import com.horizon.backend.service.MetricsService;
import com.horizon.backend.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentPushServiceImpl implements AgentPushService {

    private final AgentRepository agentRepository;
    private final ClientKeyService clientKeyService;
    private final MetricsService metricsService;
    private final SseEmitterService sseEmitterService;

    @Override
    @Transactional
    public AgentRegisterResponse registerAgent(AgentRegisterRequest request, String ipAddress) {
        log.info("Agent registration attempt - key: {}, nodeId: {}", 
                request.getKey() != null ? request.getKey().substring(0, Math.min(12, request.getKey().length())) + "..." : "null",
                request.getNodeId());
        
        Optional<ClientKey> keyOpt = clientKeyService.validateKey(request.getKey());
        if (keyOpt.isEmpty()) {
            log.warn("Agent registration failed - invalid or expired key: {}", 
                    request.getKey() != null ? request.getKey().substring(0, Math.min(12, request.getKey().length())) + "..." : "null");
            throw new BadRequestException("Invalid or expired authentication key");
        }

        ClientKey clientKey = keyOpt.get();
        clientKeyService.updateLastUsed(request.getKey());

        Optional<Agent> existingAgent = agentRepository.findByClientKeyIdAndNodeId(
                clientKey.getId(), request.getNodeId());

        Agent agent;
        if (existingAgent.isPresent()) {
            agent = existingAgent.get();
            agent.setHostname(request.getHostname());
            agent.setOs(request.getOs());
            agent.setPlatform(request.getPlatform());
            agent.setAgentIp(ipAddress);
            agent.setAgentPort(request.getPort());
            agent.setLastSeenAt(LocalDateTime.now());
            agent = agentRepository.save(agent);
            log.info("Agent updated: {} (nodeId: {})", agent.getName(), request.getNodeId());
        } else {
            String agentName = request.getHostname() != null ? request.getHostname() : request.getNodeId();

            String baseName = agentName;
            int counter = 1;
            while (agentRepository.existsByName(agentName)) {
                agentName = baseName + "-" + counter++;
            }

            agent = Agent.builder()
                    .userId(clientKey.getUserId())
                    .clientKeyId(clientKey.getId())
                    .name(agentName)
                    .nodeId(request.getNodeId())
                    .hostname(request.getHostname())
                    .os(request.getOs())
                    .platform(request.getPlatform())
                    .agentIp(ipAddress)
                    .agentPort(request.getPort())
                    .enabled(true)
                    .lastSeenAt(LocalDateTime.now())
                    .build();

            agent = agentRepository.save(agent);
            log.info("New agent registered: {} (nodeId: {}, userId: {})",
                    agent.getName(), request.getNodeId(), clientKey.getUserId());
        }

        return AgentRegisterResponse.builder()
                .id(agent.getId())
                .nodeId(agent.getNodeId())
                .hostname(agent.getHostname())
                .os(agent.getOs())
                .platform(agent.getPlatform())
                .createdAt(agent.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void pushMetrics(AgentPushRequest request, String ipAddress) {
        Optional<ClientKey> keyOpt = clientKeyService.validateKey(request.getKey());
        if (keyOpt.isEmpty()) {
            throw new BadRequestException("Invalid or expired authentication key");
        }

        ClientKey clientKey = keyOpt.get();
        AgentMetricsPayload metrics = request.getMetrics();

        Optional<Agent> agentOpt = agentRepository.findByClientKeyIdAndNodeId(
                clientKey.getId(), metrics.getNodeId());

        if (agentOpt.isEmpty()) {
            throw new BadRequestException("Agent not registered. Please run 'horizon-agent auth <key>' first.");
        }

        Agent agent = agentOpt.get();

        agentRepository.updateLastSeenAt(agent.getId(), LocalDateTime.now());
        clientKeyService.updateLastUsed(request.getKey());

        MetricsRequest metricsRequest = convertToMetricsRequest(metrics);

        metricsService.saveMetrics(agent.getId(), metricsRequest);

        RealtimeMetrics realtimeMetrics = metricsService.getRealtimeMetrics(agent.getId());
        sseEmitterService.sendToAll(realtimeMetrics);
    }

    @Override
    public boolean validateKey(String key) {
        return clientKeyService.validateKey(key).isPresent();
    }

    private MetricsRequest convertToMetricsRequest(AgentMetricsPayload payload) {
        long diskTotal = 0;
        long diskUsed = 0;
        if (payload.getDisks() != null && !payload.getDisks().isEmpty()) {
            for (AgentMetricsPayload.DiskMetric disk : payload.getDisks()) {
                if (disk.getTotalBytes() != null) diskTotal += disk.getTotalBytes();
                if (disk.getUsedBytes() != null) diskUsed += disk.getUsedBytes();
            }
        }

        long networkRx = 0;
        long networkTx = 0;
        double networkRxRate = 0;
        double networkTxRate = 0;
        if (payload.getInterfaces() != null && !payload.getInterfaces().isEmpty()) {
            for (AgentMetricsPayload.InterfaceMetric iface : payload.getInterfaces()) {
                if (iface.getRecvBytes() != null) networkRx += iface.getRecvBytes();
                if (iface.getSentBytes() != null) networkTx += iface.getSentBytes();
                if (iface.getRecvRate() != null) networkRxRate += iface.getRecvRate();
                if (iface.getSentRate() != null) networkTxRate += iface.getSentRate();
            }
        }

        return MetricsRequest.builder()
                .cpuUsage(payload.getCpuUsage() != null ? payload.getCpuUsage() : 0.0)
                .memoryTotal(payload.getMemoryTotal() != null ? payload.getMemoryTotal() : 0L)
                .memoryUsed(payload.getMemoryUsed() != null ? payload.getMemoryUsed() : 0L)
                .diskTotal(diskTotal)
                .diskUsed(diskUsed)
                .networkRxBytes(networkRx)
                .networkTxBytes(networkTx)
                .networkRxRate(networkRxRate)
                .networkTxRate(networkTxRate)
                .temperature(payload.getTemperature())
                .uptimeSeconds(payload.getUptimeSeconds())
                .processCount(payload.getProcessCount() != null ? payload.getProcessCount().intValue() : null)
                .nodeId(payload.getNodeId())
                .os(payload.getOs())
                .platform(payload.getPlatform())
                .disks(payload.getDisks() != null ? payload.getDisks().stream()
                        .map(d -> MetricsRequest.DiskInfo.builder()
                                .device(d.getDevice())
                                .mountpoint(d.getMountpoint())
                                .totalBytes(d.getTotalBytes())
                                .usedBytes(d.getUsedBytes())
                                .usage(d.getUsage())
                                .build())
                        .collect(Collectors.toList()) : null)
                .interfaces(payload.getInterfaces() != null ? payload.getInterfaces().stream()
                        .map(i -> MetricsRequest.NetworkInterfaceInfo.builder()
                                .name(i.getName())
                                .ips(i.getIps())
                                .sentBytes(i.getSentBytes())
                                .recvBytes(i.getRecvBytes())
                                .sentRate(i.getSentRate())
                                .recvRate(i.getRecvRate())
                                .build())
                        .collect(Collectors.toList()) : null)
                .build();
    }
}

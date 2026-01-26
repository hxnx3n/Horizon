package com.horizon.backend.service;

import com.horizon.backend.dto.metrics.RealtimeMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseEmitterService {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L;
    private final Map<Long, List<SseEmitter>> agentEmitters = new ConcurrentHashMap<>();
    private final List<SseEmitter> allAgentsEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        allAgentsEmitters.add(emitter);

        emitter.onCompletion(() -> {
            allAgentsEmitters.remove(emitter);
            log.debug("SSE connection completed for all agents");
        });

        emitter.onTimeout(() -> {
            allAgentsEmitters.remove(emitter);
            log.debug("SSE connection timed out for all agents");
        });

        emitter.onError(e -> {
            allAgentsEmitters.remove(emitter);
            log.debug("SSE connection error for all agents: {}", e.getMessage());
        });

        return emitter;
    }

    public SseEmitter createEmitterForAgent(Long agentId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        agentEmitters.computeIfAbsent(agentId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> {
            removeEmitter(agentId, emitter);
            log.debug("SSE connection completed for agent: {}", agentId);
        });

        emitter.onTimeout(() -> {
            removeEmitter(agentId, emitter);
            log.debug("SSE connection timed out for agent: {}", agentId);
        });

        emitter.onError(e -> {
            removeEmitter(agentId, emitter);
            log.debug("SSE connection error for agent {}: {}", agentId, e.getMessage());
        });

        return emitter;
    }

    public void sendToAgent(Long agentId, RealtimeMetrics metrics) {
        List<SseEmitter> emitters = agentEmitters.get(agentId);
        if (emitters != null) {
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("metrics")
                            .data(metrics));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }

            emitters.removeAll(deadEmitters);
        }
    }

    public void sendToAll(RealtimeMetrics metrics) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : allAgentsEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("metrics")
                        .data(metrics));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        allAgentsEmitters.removeAll(deadEmitters);

        sendToAgent(metrics.getAgentId(), metrics);
    }

    public void sendToAll(List<RealtimeMetrics> metricsList) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : allAgentsEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("metrics-all")
                        .data(metricsList));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        allAgentsEmitters.removeAll(deadEmitters);
    }

    public void sendHeartbeat() {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : allAgentsEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        allAgentsEmitters.removeAll(deadEmitters);

        for (Map.Entry<Long, List<SseEmitter>> entry : agentEmitters.entrySet()) {
            List<SseEmitter> agentDeadEmitters = new CopyOnWriteArrayList<>();

            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("ping"));
                } catch (IOException e) {
                    agentDeadEmitters.add(emitter);
                }
            }

            entry.getValue().removeAll(agentDeadEmitters);
        }
    }

    public int getConnectionCount() {
        int count = allAgentsEmitters.size();
        for (List<SseEmitter> emitters : agentEmitters.values()) {
            count += emitters.size();
        }
        return count;
    }

    private void removeEmitter(Long agentId, SseEmitter emitter) {
        List<SseEmitter> emitters = agentEmitters.get(agentId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                agentEmitters.remove(agentId);
            }
        }
    }
}

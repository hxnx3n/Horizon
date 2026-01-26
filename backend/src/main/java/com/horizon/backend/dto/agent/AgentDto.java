package com.horizon.backend.dto.agent;

import com.horizon.backend.entity.Agent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDto {

    private Long id;
    private String name;
    private String ip;
    private Integer port;
    private Long pollingInterval;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AgentDto from(Agent agent) {
        return AgentDto.builder()
                .id(agent.getId())
                .name(agent.getName())
                .ip(agent.getIp())
                .port(agent.getPort())
                .pollingInterval(agent.getPollingInterval())
                .enabled(agent.isEnabled())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }
}

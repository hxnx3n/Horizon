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
    private Long userId;
    private Long clientKeyId;
    private String name;
    private String nodeId;
    private String hostname;
    private String os;
    private String platform;
    private boolean enabled;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AgentDto from(Agent agent) {
        return AgentDto.builder()
                .id(agent.getId())
                .userId(agent.getUserId())
                .clientKeyId(agent.getClientKeyId())
                .name(agent.getName())
                .nodeId(agent.getNodeId())
                .hostname(agent.getHostname())
                .os(agent.getOs())
                .platform(agent.getPlatform())
                .enabled(agent.isEnabled())
                .lastSeenAt(agent.getLastSeenAt())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }
}


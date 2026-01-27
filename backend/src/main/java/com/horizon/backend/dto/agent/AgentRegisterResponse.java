package com.horizon.backend.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRegisterResponse {

    private Long id;
    private String nodeId;
    private String hostname;
    private String os;
    private String platform;
    private LocalDateTime createdAt;
}

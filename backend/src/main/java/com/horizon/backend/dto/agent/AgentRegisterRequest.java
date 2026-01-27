package com.horizon.backend.dto.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRegisterRequest {

    @NotBlank(message = "Key is required")
    private String key;

    @NotBlank(message = "Node ID is required")
    private String nodeId;

    private String hostname;
    
    private String os;
    
    private String platform;
}

package com.horizon.backend.dto.agent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPushRequest {

    @NotBlank(message = "Key is required")
    private String key;

    @NotNull(message = "Metrics are required")
    @Valid
    private AgentMetricsPayload metrics;
}

package com.horizon.backend.dto.agent;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentUpdateRequest {

    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @Pattern(
            regexp = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
            message = "Invalid IP address format"
    )
    private String ip;

    @Min(value = 1, message = "Port must be at least 1")
    @Max(value = 65535, message = "Port must be at most 65535")
    private Integer port;

    @Min(value = 100, message = "Polling interval must be at least 100ms")
    private Long pollingInterval;

    private Boolean enabled;
}

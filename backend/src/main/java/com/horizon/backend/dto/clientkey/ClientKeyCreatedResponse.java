package com.horizon.backend.dto.clientkey;

import com.horizon.backend.entity.ClientKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientKeyCreatedResponse {
    private Long id;
    private String name;
    private String description;
    private String keyValue;
    private boolean enabled;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public static ClientKeyCreatedResponse from(ClientKey clientKey) {
        return ClientKeyCreatedResponse.builder()
                .id(clientKey.getId())
                .name(clientKey.getName())
                .description(clientKey.getDescription())
                .keyValue(clientKey.getKeyValue())
                .enabled(clientKey.isEnabled())
                .expiresAt(clientKey.getExpiresAt())
                .createdAt(clientKey.getCreatedAt())
                .build();
    }
}

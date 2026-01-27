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
public class ClientKeyDto {
    private Long id;
    private String name;
    private String description;
    private boolean enabled;
    private boolean expired;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private String maskedKey;

    public static ClientKeyDto from(ClientKey clientKey) {
        return ClientKeyDto.builder()
                .id(clientKey.getId())
                .name(clientKey.getName())
                .description(clientKey.getDescription())
                .enabled(clientKey.isEnabled())
                .expired(clientKey.isExpired())
                .expiresAt(clientKey.getExpiresAt())
                .lastUsedAt(clientKey.getLastUsedAt())
                .createdAt(clientKey.getCreatedAt())
                .maskedKey(maskKey(clientKey.getKeyValue()))
                .build();
    }

    private static String maskKey(String keyValue) {
        if (keyValue == null || keyValue.length() < 8) {
            return "****";
        }
        return keyValue.substring(0, 8) + "..." + keyValue.substring(keyValue.length() - 4);
    }
}

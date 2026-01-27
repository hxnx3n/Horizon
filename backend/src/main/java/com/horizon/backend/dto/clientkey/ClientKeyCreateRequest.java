package com.horizon.backend.dto.clientkey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientKeyCreateRequest {

    @NotBlank(message = "Key name is required")
    @Size(max = 100, message = "Key name must be at most 100 characters")
    private String name;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    private Integer expiresInDays;
}

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

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    private Integer expiresInDays;
}

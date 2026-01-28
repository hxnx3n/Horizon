package com.horizon.backend.dto.clientkey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientKeyCreateRequest {

    private Integer expiresInDays;
}

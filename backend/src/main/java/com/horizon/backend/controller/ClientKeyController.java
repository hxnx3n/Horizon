package com.horizon.backend.controller;

import com.horizon.backend.common.ApiResponse;
import com.horizon.backend.dto.clientkey.ClientKeyCreateRequest;
import com.horizon.backend.dto.clientkey.ClientKeyCreatedResponse;
import com.horizon.backend.dto.clientkey.ClientKeyDto;
import com.horizon.backend.service.AuthService;
import com.horizon.backend.service.ClientKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/client-keys")
@RequiredArgsConstructor
public class ClientKeyController {

    private final ClientKeyService clientKeyService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<ApiResponse<ClientKeyCreatedResponse>> createKey(
            @Valid @RequestBody ClientKeyCreateRequest request) {
        Long userId = authService.getCurrentUser().getId();
        ClientKeyCreatedResponse response = clientKeyService.createKey(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Client key created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientKeyDto>>> getMyKeys() {
        Long userId = authService.getCurrentUser().getId();
        List<ClientKeyDto> keys = clientKeyService.getKeysByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(keys, "Client keys retrieved successfully"));
    }

    @PostMapping("/{keyId}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeKey(@PathVariable Long keyId) {
        Long userId = authService.getCurrentUser().getId();
        clientKeyService.revokeKey(userId, keyId);
        return ResponseEntity.ok(ApiResponse.success("Client key revoked successfully"));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<ApiResponse<Void>> deleteKey(@PathVariable Long keyId) {
        Long userId = authService.getCurrentUser().getId();
        clientKeyService.deleteKey(userId, keyId);
        return ResponseEntity.ok(ApiResponse.success("Client key deleted successfully"));
    }
}

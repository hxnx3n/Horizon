package com.horizon.backend.service;

import com.horizon.backend.dto.clientkey.ClientKeyCreateRequest;
import com.horizon.backend.dto.clientkey.ClientKeyCreatedResponse;
import com.horizon.backend.dto.clientkey.ClientKeyDto;
import com.horizon.backend.entity.ClientKey;

import java.util.List;
import java.util.Optional;

public interface ClientKeyService {

    ClientKeyCreatedResponse createKey(Long userId, ClientKeyCreateRequest request);

    List<ClientKeyDto> getKeysByUserId(Long userId);

    Optional<ClientKey> validateKey(String keyValue);

    void updateLastUsed(String keyValue);

    void revokeKey(Long userId, Long keyId);

    void deleteKey(Long userId, Long keyId);
}

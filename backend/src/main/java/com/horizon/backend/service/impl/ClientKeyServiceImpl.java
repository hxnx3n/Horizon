package com.horizon.backend.service.impl;

import com.horizon.backend.dto.clientkey.ClientKeyCreateRequest;
import com.horizon.backend.dto.clientkey.ClientKeyCreatedResponse;
import com.horizon.backend.dto.clientkey.ClientKeyDto;
import com.horizon.backend.entity.ClientKey;
import com.horizon.backend.exception.ResourceNotFoundException;
import com.horizon.backend.repository.ClientKeyRepository;
import com.horizon.backend.service.ClientKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientKeyServiceImpl implements ClientKeyService {

    private final ClientKeyRepository clientKeyRepository;
    private static final String KEY_PREFIX = "hzn_";
    private static final int KEY_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ClientKeyCreatedResponse createKey(Long userId, ClientKeyCreateRequest request) {
        if (clientKeyRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new IllegalArgumentException("A key with this name already exists");
        }

        String keyValue = generateKey();
        while (clientKeyRepository.existsByKeyValue(keyValue)) {
            keyValue = generateKey();
        }

        LocalDateTime expiresAt = null;
        if (request.getExpiresInDays() != null && request.getExpiresInDays() > 0) {
            expiresAt = LocalDateTime.now().plusDays(request.getExpiresInDays());
        }

        ClientKey clientKey = ClientKey.builder()
                .userId(userId)
                .keyValue(keyValue)
                .name(request.getName())
                .description(request.getDescription())
                .enabled(true)
                .expiresAt(expiresAt)
                .build();

        ClientKey savedKey = clientKeyRepository.save(clientKey);
        log.info("Created new client key for user: {} with name: {}", userId, request.getName());

        return ClientKeyCreatedResponse.from(savedKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientKeyDto> getKeysByUserId(Long userId) {
        return clientKeyRepository.findByUserId(userId).stream()
                .map(ClientKeyDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientKey> validateKey(String keyValue) {
        return clientKeyRepository.findValidKey(keyValue, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void updateLastUsed(String keyValue) {
        clientKeyRepository.updateLastUsedAt(keyValue, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void revokeKey(Long userId, Long keyId) {
        ClientKey clientKey = clientKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("Client key not found"));

        if (!clientKey.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Key does not belong to this user");
        }

        clientKey.setEnabled(false);
        clientKeyRepository.save(clientKey);
        log.info("Revoked client key: {} for user: {}", keyId, userId);
    }

    @Override
    @Transactional
    public void deleteKey(Long userId, Long keyId) {
        ClientKey clientKey = clientKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("Client key not found"));

        if (!clientKey.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Key does not belong to this user");
        }

        clientKeyRepository.delete(clientKey);
        log.info("Deleted client key: {} for user: {}", keyId, userId);
    }

    private String generateKey() {
        byte[] randomBytes = new byte[KEY_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return KEY_PREFIX + encoded;
    }
}

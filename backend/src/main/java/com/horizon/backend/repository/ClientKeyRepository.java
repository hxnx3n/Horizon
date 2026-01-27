package com.horizon.backend.repository;

import com.horizon.backend.entity.ClientKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientKeyRepository extends JpaRepository<ClientKey, Long> {

    Optional<ClientKey> findByKeyValue(String keyValue);

    @Query("SELECT ck FROM ClientKey ck WHERE ck.keyValue = :keyValue AND ck.enabled = true " +
           "AND (ck.expiresAt IS NULL OR ck.expiresAt > :now)")
    Optional<ClientKey> findValidKey(@Param("keyValue") String keyValue, @Param("now") LocalDateTime now);

    List<ClientKey> findByUserId(Long userId);

    List<ClientKey> findByUserIdAndEnabled(Long userId, boolean enabled);

    boolean existsByKeyValue(String keyValue);

    boolean existsByUserIdAndName(Long userId, String name);

    @Modifying
    @Query("UPDATE ClientKey ck SET ck.lastUsedAt = :lastUsedAt WHERE ck.keyValue = :keyValue")
    void updateLastUsedAt(@Param("keyValue") String keyValue, @Param("lastUsedAt") LocalDateTime lastUsedAt);

    @Query("SELECT COUNT(ck) FROM ClientKey ck WHERE ck.userId = :userId AND ck.enabled = true")
    long countActiveKeysByUserId(@Param("userId") Long userId);
}

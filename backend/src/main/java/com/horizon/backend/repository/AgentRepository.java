package com.horizon.backend.repository;

import com.horizon.backend.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByName(String name);

    Optional<Agent> findByNodeId(String nodeId);

    Optional<Agent> findByClientKeyIdAndNodeId(Long clientKeyId, String nodeId);

    boolean existsByName(String name);

    boolean existsByNodeId(String nodeId);

    boolean existsByClientKeyIdAndNodeId(Long clientKeyId, String nodeId);

    List<Agent> findByEnabled(boolean enabled);

    List<Agent> findByUserId(Long userId);

    List<Agent> findByUserIdAndEnabled(Long userId, boolean enabled);

    List<Agent> findByClientKeyId(Long clientKeyId);

    @Modifying
    @Query("UPDATE Agent a SET a.lastSeenAt = :lastSeenAt WHERE a.id = :agentId")
    void updateLastSeenAt(@Param("agentId") Long agentId, @Param("lastSeenAt") LocalDateTime lastSeenAt);
}

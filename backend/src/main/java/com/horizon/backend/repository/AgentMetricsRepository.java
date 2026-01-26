package com.horizon.backend.repository;

import com.horizon.backend.entity.AgentMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentMetricsRepository extends JpaRepository<AgentMetrics, Long> {

    List<AgentMetrics> findByAgentIdOrderByCreatedAtDesc(Long agentId);

    Page<AgentMetrics> findByAgentIdOrderByCreatedAtDesc(Long agentId, Pageable pageable);

    Optional<AgentMetrics> findTopByAgentIdOrderByCreatedAtDesc(Long agentId);

    List<AgentMetrics> findByAgentIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long agentId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    @Query("SELECT m FROM AgentMetrics m WHERE m.agent.id = :agentId AND m.createdAt >= :since ORDER BY m.createdAt DESC")
    List<AgentMetrics> findRecentByAgentId(@Param("agentId") Long agentId, @Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM AgentMetrics m WHERE m.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);

    @Modifying
    @Query("DELETE FROM AgentMetrics m WHERE m.agent.id = :agentId")
    int deleteByAgentId(@Param("agentId") Long agentId);

    long countByAgentId(Long agentId);
}

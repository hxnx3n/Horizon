package com.horizon.backend.repository;

import com.horizon.backend.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByName(String name);

    Optional<Agent> findByIp(String ip);

    boolean existsByName(String name);

    boolean existsByIp(String ip);

    List<Agent> findByEnabled(boolean enabled);
}

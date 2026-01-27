package com.horizon.backend.service;

import com.horizon.backend.dto.agent.AgentDto;

import java.util.List;

public interface AgentService {

    AgentDto getAgent(Long id);

    List<AgentDto> getAllAgents();

    List<AgentDto> getAgentsByUserId(Long userId);

    void deleteAgent(Long id);
}

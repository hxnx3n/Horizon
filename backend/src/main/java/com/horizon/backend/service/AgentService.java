package com.horizon.backend.service;

import com.horizon.backend.dto.agent.AgentCreateRequest;
import com.horizon.backend.dto.agent.AgentDto;
import com.horizon.backend.dto.agent.AgentUpdateRequest;

import java.util.List;

public interface AgentService {

    AgentDto createAgent(AgentCreateRequest request);

    AgentDto getAgent(Long id);

    AgentDto getAgentByName(String name);

    AgentDto getAgentByIp(String ip);

    List<AgentDto> getAllAgents();

    List<AgentDto> getEnabledAgents();

    AgentDto updateAgent(Long id, AgentUpdateRequest request);

    void deleteAgent(Long id);
}

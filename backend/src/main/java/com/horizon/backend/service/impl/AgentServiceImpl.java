package com.horizon.backend.service.impl;

import com.horizon.backend.dto.agent.AgentDto;
import com.horizon.backend.entity.Agent;
import com.horizon.backend.exception.ResourceNotFoundException;
import com.horizon.backend.repository.AgentRepository;
import com.horizon.backend.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;

    @Override
    @Transactional(readOnly = true)
    public AgentDto getAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", id));
        return AgentDto.from(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentDto> getAllAgents() {
        return agentRepository.findAll().stream()
                .map(AgentDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentDto> getAgentsByUserId(Long userId) {
        return agentRepository.findByUserId(userId).stream()
                .map(AgentDto::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", id));

        agentRepository.delete(agent);
        log.info("Agent deleted successfully: {}", agent.getName());
    }
}


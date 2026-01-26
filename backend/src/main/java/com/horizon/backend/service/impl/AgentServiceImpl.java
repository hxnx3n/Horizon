package com.horizon.backend.service.impl;

import com.horizon.backend.dto.agent.AgentCreateRequest;
import com.horizon.backend.dto.agent.AgentDto;
import com.horizon.backend.dto.agent.AgentUpdateRequest;
import com.horizon.backend.entity.Agent;
import com.horizon.backend.exception.DuplicateResourceException;
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
    @Transactional
    public AgentDto createAgent(AgentCreateRequest request) {
        if (agentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Agent", "name", request.getName());
        }

        if (agentRepository.existsByIp(request.getIp())) {
            throw new DuplicateResourceException("Agent", "ip", request.getIp());
        }

        Agent agent = Agent.builder()
                .name(request.getName())
                .ip(request.getIp())
                .port(request.getPort() != null ? request.getPort() : 9090)
                .pollingInterval(request.getPollingInterval() != null ? request.getPollingInterval() : 1000L)
                .enabled(true)
                .build();

        Agent savedAgent = agentRepository.save(agent);
        log.info("Agent created successfully: {} ({})", savedAgent.getName(), savedAgent.getIp());

        return AgentDto.from(savedAgent);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentDto getAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", id));
        return AgentDto.from(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentDto getAgentByName(String name) {
        Agent agent = agentRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "name", name));
        return AgentDto.from(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentDto getAgentByIp(String ip) {
        Agent agent = agentRepository.findByIp(ip)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "ip", ip));
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
    public List<AgentDto> getEnabledAgents() {
        return agentRepository.findByEnabled(true).stream()
                .map(AgentDto::from)
                .toList();
    }

    @Override
    @Transactional
    public AgentDto updateAgent(Long id, AgentUpdateRequest request) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", id));

        if (request.getName() != null && !request.getName().equals(agent.getName())) {
            if (agentRepository.existsByName(request.getName())) {
                throw new DuplicateResourceException("Agent", "name", request.getName());
            }
            agent.setName(request.getName());
        }

        if (request.getIp() != null && !request.getIp().equals(agent.getIp())) {
            if (agentRepository.existsByIp(request.getIp())) {
                throw new DuplicateResourceException("Agent", "ip", request.getIp());
            }
            agent.setIp(request.getIp());
        }

        if (request.getPort() != null) {
            agent.setPort(request.getPort());
        }

        if (request.getPollingInterval() != null) {
            agent.setPollingInterval(request.getPollingInterval());
        }

        if (request.getEnabled() != null) {
            agent.setEnabled(request.getEnabled());
        }

        Agent updatedAgent = agentRepository.save(agent);
        log.info("Agent updated successfully: {} ({})", updatedAgent.getName(), updatedAgent.getIp());

        return AgentDto.from(updatedAgent);
    }

    @Override
    @Transactional
    public void deleteAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", id));

        agentRepository.delete(agent);
        log.info("Agent deleted successfully: {} ({})", agent.getName(), agent.getIp());
    }
}

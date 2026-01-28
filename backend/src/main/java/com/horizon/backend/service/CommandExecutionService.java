package com.horizon.backend.service;

import com.horizon.backend.dto.CommandExecutionResponse;
import com.horizon.backend.entity.Agent;
import com.horizon.backend.exception.ResourceNotFoundException;
import com.horizon.backend.repository.AgentRepository;
import com.horizon.backend.websocket.AgentWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandExecutionService {

    private final AgentRepository agentRepository;
    private final AgentWebSocketHandler agentWebSocketHandler;

    public CommandExecutionResponse executeCommand(String agentId, String command) {
        if (command == null || command.trim().isEmpty()) {
            return new CommandExecutionResponse("", "Command cannot be empty", 1);
        }

        Long id;
        try {
            id = Long.parseLong(agentId);
        } catch (NumberFormatException e) {
            return new CommandExecutionResponse("", "Invalid agent ID", 1);
        }

        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", id));

        if (!agentWebSocketHandler.isAgentConnected(agentId)) {
            log.warn("Agent {} is not connected via WebSocket", agentId);
            return new CommandExecutionResponse("", "Agent is offline", 1);
        }

        log.info("Executing command on agent {}: {}", agentId, command);

        try {
            agentWebSocketHandler.sendCommandToAgent(agentId, command);
            return new CommandExecutionResponse("Command sent to agent, waiting for response...", "", 0);
        } catch (IOException e) {
            log.error("Error sending command to agent {}: {}", agentId, e.getMessage(), e);
            return new CommandExecutionResponse(
                    "",
                    "Failed to send command to agent: " + e.getMessage(),
                    -1);
        } catch (Exception e) {
            log.error("Unexpected error executing command on agent {}: {}", agentId, e.getMessage(), e);
            return new CommandExecutionResponse(
                    "",
                    "Command execution failed: " + e.getMessage(),
                    -1);
        }
    }
}

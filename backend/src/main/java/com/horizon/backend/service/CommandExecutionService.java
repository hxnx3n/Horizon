package com.horizon.backend.service;

import com.horizon.backend.dto.CommandExecutionResponse;
import com.horizon.backend.entity.Agent;
import com.horizon.backend.exception.ResourceNotFoundException;
import com.horizon.backend.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandExecutionService {

    private final AgentRepository agentRepository;
    private final RestTemplate restTemplate;

    private static final long COMMAND_TIMEOUT = 30; // seconds
    private static final int MAX_OUTPUT_LENGTH = 50000; // characters

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

        if (agent.getAgentIp() == null || agent.getAgentPort() == null) {
            return new CommandExecutionResponse("", "Agent IP or port not configured", 1);
        }

        log.info("Executing command on agent {}: {}", agentId, command);

        try {
            String url = String.format("http://%s:%d/command", agent.getAgentIp(), agent.getAgentPort());
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("command", command);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            CommandExecutionResponse response = restTemplate.postForObject(url, request, CommandExecutionResponse.class);
            
            if (response == null) {
                return new CommandExecutionResponse("", "No response from agent", -1);
            }

            if (response.getOutput() != null && response.getOutput().length() > MAX_OUTPUT_LENGTH) {
                response.setOutput(response.getOutput().substring(0, MAX_OUTPUT_LENGTH) + "\n... (output truncated)");
            }

            log.info("Command executed on agent {}: exit code = {}", agentId, response.getExitCode());
            return response;

        } catch (RestClientException e) {
            log.error("Error executing command on agent {}: {}", agentId, e.getMessage());
            return new CommandExecutionResponse(
                    "", 
                    "Failed to communicate with agent: " + e.getMessage(), 
                    -1
            );
        } catch (Exception e) {
            log.error("Unexpected error executing command on agent {}: {}", agentId, e.getMessage());
            return new CommandExecutionResponse(
                    "", 
                    "Command execution failed: " + e.getMessage(), 
                    -1
            );
        }
    }
}


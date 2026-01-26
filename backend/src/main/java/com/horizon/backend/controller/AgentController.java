package com.horizon.backend.controller;

import com.horizon.backend.common.ApiResponse;
import com.horizon.backend.dto.agent.AgentCreateRequest;
import com.horizon.backend.dto.agent.AgentDto;
import com.horizon.backend.dto.agent.AgentUpdateRequest;
import com.horizon.backend.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    public ResponseEntity<ApiResponse<AgentDto>> createAgent(
            @Valid @RequestBody AgentCreateRequest request) {
        AgentDto agent = agentService.createAgent(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(agent, "Agent created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgentDto>> getAgent(@PathVariable Long id) {
        AgentDto agent = agentService.getAgent(id);
        return ResponseEntity.ok(ApiResponse.success(agent, "Agent retrieved successfully"));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse<AgentDto>> getAgentByName(@PathVariable String name) {
        AgentDto agent = agentService.getAgentByName(name);
        return ResponseEntity.ok(ApiResponse.success(agent, "Agent retrieved successfully"));
    }

    @GetMapping("/ip/{ip}")
    public ResponseEntity<ApiResponse<AgentDto>> getAgentByIp(@PathVariable String ip) {
        AgentDto agent = agentService.getAgentByIp(ip);
        return ResponseEntity.ok(ApiResponse.success(agent, "Agent retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgentDto>>> getAllAgents(
            @RequestParam(required = false) Boolean enabled) {
        List<AgentDto> agents;
        if (Boolean.TRUE.equals(enabled)) {
            agents = agentService.getEnabledAgents();
        } else {
            agents = agentService.getAllAgents();
        }
        return ResponseEntity.ok(ApiResponse.success(agents, "Agents retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AgentDto>> updateAgent(
            @PathVariable Long id,
            @Valid @RequestBody AgentUpdateRequest request) {
        AgentDto agent = agentService.updateAgent(id, request);
        return ResponseEntity.ok(ApiResponse.success(agent, "Agent updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAgent(@PathVariable Long id) {
        agentService.deleteAgent(id);
        return ResponseEntity.ok(ApiResponse.success("Agent deleted successfully"));
    }
}

package com.horizon.backend.controller;

import com.horizon.backend.common.ApiResponse;
import com.horizon.backend.dto.agent.AgentDto;
import com.horizon.backend.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgentDto>> getAgent(@PathVariable Long id) {
        AgentDto agent = agentService.getAgent(id);
        return ResponseEntity.ok(ApiResponse.success(agent, "Agent retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgentDto>>> getAllAgents() {
        List<AgentDto> agents = agentService.getAllAgents();
        return ResponseEntity.ok(ApiResponse.success(agents, "Agents retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAgent(@PathVariable Long id) {
        agentService.deleteAgent(id);
        return ResponseEntity.ok(ApiResponse.success("Agent deleted successfully"));
    }
}


package com.horizon.backend.controller;

import com.horizon.backend.dto.CommandExecutionRequest;
import com.horizon.backend.dto.CommandExecutionResponse;
import com.horizon.backend.service.CommandExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentCommandController {

    private final CommandExecutionService commandExecutionService;

    @PostMapping("/{agentId}/command")
    public ResponseEntity<CommandExecutionResponse> executeCommand(
            @PathVariable String agentId,
            @RequestBody CommandExecutionRequest request) {
        try {
            CommandExecutionResponse response = commandExecutionService.executeCommand(agentId, request.getCommand());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new CommandExecutionResponse("", e.getMessage(), 1)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new CommandExecutionResponse("", "Command execution failed: " + e.getMessage(), -1)
            );
        }
    }
}

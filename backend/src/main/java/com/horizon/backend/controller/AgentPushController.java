package com.horizon.backend.controller;

import com.horizon.backend.common.ApiResponse;
import com.horizon.backend.dto.agent.AgentPushRequest;
import com.horizon.backend.dto.agent.AgentRegisterRequest;
import com.horizon.backend.dto.agent.AgentRegisterResponse;
import com.horizon.backend.service.AgentPushService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentPushController {

    private final AgentPushService agentPushService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AgentRegisterResponse>> registerAgent(
            @Valid @RequestBody AgentRegisterRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        log.debug("Agent registration request from IP: {}, nodeId: {}", ipAddress, request.getNodeId());

        AgentRegisterResponse response = agentPushService.registerAgent(request, ipAddress);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Agent registered successfully"));
    }

    @PostMapping("/push")
    public ResponseEntity<ApiResponse<Void>> pushMetrics(
            @Valid @RequestBody AgentPushRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        
        agentPushService.pushMetrics(request, ipAddress);
        
        return ResponseEntity.ok(ApiResponse.success("Metrics received successfully"));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateKey(@RequestBody String key) {
        boolean isValid = agentPushService.validateKey(key.replace("\"", "").trim());
        return ResponseEntity.ok(ApiResponse.success(isValid, "Key validation complete"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

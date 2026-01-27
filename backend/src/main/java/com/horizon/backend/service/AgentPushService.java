package com.horizon.backend.service;

import com.horizon.backend.dto.agent.AgentPushRequest;
import com.horizon.backend.dto.agent.AgentRegisterRequest;
import com.horizon.backend.dto.agent.AgentRegisterResponse;

public interface AgentPushService {

    AgentRegisterResponse registerAgent(AgentRegisterRequest request, String ipAddress);

    void pushMetrics(AgentPushRequest request, String ipAddress);

    boolean validateKey(String key);
}

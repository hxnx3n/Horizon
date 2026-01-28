package com.horizon.backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, WebSocketSession> agentSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String agentId = extractAgentId(session);
        if (agentId != null) {
            agentSessions.put(agentId, session);
            log.info("Agent {} connected via WebSocket", agentId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String agentId = extractAgentId(session);
        log.debug("Message from agent {}: {}", agentId, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String agentId = extractAgentId(session);
        if (agentId != null) {
            agentSessions.remove(agentId);
            log.info("Agent {} disconnected", agentId);
        }
    }

    public void sendCommandToAgent(String agentId, String command) throws IOException {
        WebSocketSession session = agentSessions.get(agentId);
        if (session == null || !session.isOpen()) {
            throw new IOException("Agent " + agentId + " is not connected");
        }

        AgentMessage message = new AgentMessage("command", command);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        log.info("Sent command to agent {}: {}", agentId, command);
    }

    public boolean isAgentConnected(String agentId) {
        WebSocketSession session = agentSessions.get(agentId);
        return session != null && session.isOpen();
    }

    private String extractAgentId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("agentId=")) {
                    return param.substring(8);
                }
            }
        }
        return null;
    }

    public static class AgentMessage {
        public String type;
        public String data;

        public AgentMessage(String type, String data) {
            this.type = type;
            this.data = data;
        }
    }
}

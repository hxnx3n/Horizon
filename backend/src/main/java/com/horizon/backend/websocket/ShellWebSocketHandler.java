package com.horizon.backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShellWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentWebSocketHandler agentWebSocketHandler;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String agentId = extractAgentId(session);
        log.info("Frontend shell session connected for agent {}", agentId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String agentId = extractAgentId(session);
        String payload = message.getPayload();

        try {
            JsonNode json = objectMapper.readTree(payload);
            String type = json.has("type") ? json.get("type").asText() : "";

            switch (type) {
                case "shell_start":
                    int cols = json.has("cols") ? json.get("cols").asInt() : 80;
                    int rows = json.has("rows") ? json.get("rows").asInt() : 24;
                    agentWebSocketHandler.startShell(agentId, session, cols, rows);
                    break;
                case "shell_input":
                    String data = json.has("data") ? json.get("data").asText() : "";
                    agentWebSocketHandler.sendShellInput(agentId, data);
                    break;
                case "shell_resize":
                    int newCols = json.has("cols") ? json.get("cols").asInt() : 80;
                    int newRows = json.has("rows") ? json.get("rows").asInt() : 24;
                    agentWebSocketHandler.resizeShell(agentId, newCols, newRows);
                    break;
                case "shell_stop":
                    agentWebSocketHandler.stopShell(agentId, session);
                    break;
                default:
                    log.warn("Unknown message type from frontend: {}", type);
            }
        } catch (Exception e) {
            log.error("Error handling frontend shell message: {}", e.getMessage());
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"data\":\"" + e.getMessage() + "\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String agentId = extractAgentId(session);
        if (agentId != null) {
            agentWebSocketHandler.removeShellListener(agentId, session);
            log.info("Frontend shell session closed for agent {}", agentId);
        }
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
}

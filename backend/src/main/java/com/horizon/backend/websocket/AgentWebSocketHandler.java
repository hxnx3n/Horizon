package com.horizon.backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, WebSocketSession> agentSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<CommandResult>> pendingCommands = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<WebSocketSession>> shellListeners = new ConcurrentHashMap<>();

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
        String payload = message.getPayload();
        log.debug("Message from agent {}: {}", agentId, payload);

        try {
            JsonNode json = objectMapper.readTree(payload);
            String type = json.has("type") ? json.get("type").asText() : "";

            if ("command_result".equals(type)) {
                CommandResult result = new CommandResult(
                        json.has("output") ? json.get("output").asText() : "",
                        json.has("error") ? json.get("error").asText() : "",
                        json.has("code") ? json.get("code").asInt() : 0
                );

                CompletableFuture<CommandResult> future = pendingCommands.remove(agentId);
                if (future != null) {
                    future.complete(result);
                    log.info("Command result received from agent {}: exitCode={}", agentId, result.exitCode);
                }
            } else if ("shell_output".equals(type) || "shell_exit".equals(type)) {
                forwardToShellListeners(agentId, payload);
            }
        } catch (Exception e) {
            log.error("Error parsing message from agent {}: {}", agentId, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String agentId = extractAgentId(session);
        if (agentId != null) {
            agentSessions.remove(agentId);
            CompletableFuture<CommandResult> future = pendingCommands.remove(agentId);
            if (future != null) {
                future.completeExceptionally(new IOException("Agent disconnected"));
            }
            log.info("Agent {} disconnected", agentId);
        }
    }

    public CommandResult sendCommandAndWait(String agentId, String command, long timeoutSeconds) throws Exception {
        WebSocketSession session = agentSessions.get(agentId);
        if (session == null || !session.isOpen()) {
            throw new IOException("Agent " + agentId + " is not connected");
        }

        CompletableFuture<CommandResult> future = new CompletableFuture<>();
        pendingCommands.put(agentId, future);

        try {
            AgentMessage message = new AgentMessage("command", command);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            log.info("Sent command to agent {}: {}", agentId, command);

            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            pendingCommands.remove(agentId);
            throw new IOException("Command timeout after " + timeoutSeconds + " seconds");
        } catch (Exception e) {
            pendingCommands.remove(agentId);
            throw e;
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

    public void startShell(String agentId, WebSocketSession frontendSession, int cols, int rows) throws IOException {
        WebSocketSession agentSession = agentSessions.get(agentId);
        if (agentSession == null || !agentSession.isOpen()) {
            throw new IOException("Agent " + agentId + " is not connected");
        }

        shellListeners.computeIfAbsent(agentId, k -> new CopyOnWriteArraySet<>()).add(frontendSession);

        ShellMessage message = new ShellMessage("shell_start", "", cols, rows);
        agentSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        log.info("Started interactive shell for agent {}", agentId);
    }

    public void sendShellInput(String agentId, String input) throws IOException {
        WebSocketSession agentSession = agentSessions.get(agentId);
        if (agentSession == null || !agentSession.isOpen()) {
            throw new IOException("Agent " + agentId + " is not connected");
        }

        ShellMessage message = new ShellMessage("shell_input", input, 0, 0);
        agentSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    public void resizeShell(String agentId, int cols, int rows) throws IOException {
        WebSocketSession agentSession = agentSessions.get(agentId);
        if (agentSession == null || !agentSession.isOpen()) {
            throw new IOException("Agent " + agentId + " is not connected");
        }

        ShellMessage message = new ShellMessage("shell_resize", "", cols, rows);
        agentSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    public void stopShell(String agentId, WebSocketSession frontendSession) throws IOException {
        Set<WebSocketSession> listeners = shellListeners.get(agentId);
        if (listeners != null) {
            listeners.remove(frontendSession);
        }

        WebSocketSession agentSession = agentSessions.get(agentId);
        if (agentSession != null && agentSession.isOpen()) {
            ShellMessage message = new ShellMessage("shell_stop", "", 0, 0);
            agentSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            log.info("Stopped interactive shell for agent {}", agentId);
        }
    }

    public void removeShellListener(String agentId, WebSocketSession frontendSession) {
        Set<WebSocketSession> listeners = shellListeners.get(agentId);
        if (listeners != null) {
            listeners.remove(frontendSession);
        }
    }

    private void forwardToShellListeners(String agentId, String payload) {
        Set<WebSocketSession> listeners = shellListeners.get(agentId);
        if (listeners != null) {
            for (WebSocketSession listener : listeners) {
                if (listener.isOpen()) {
                    try {
                        listener.sendMessage(new TextMessage(payload));
                    } catch (IOException e) {
                        log.error("Failed to forward shell output to frontend: {}", e.getMessage());
                    }
                }
            }
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

    public static class AgentMessage {
        public String type;
        public String data;

        public AgentMessage(String type, String data) {
            this.type = type;
            this.data = data;
        }
    }

    public static class ShellMessage {
        public String type;
        public String data;
        public int cols;
        public int rows;

        public ShellMessage(String type, String data, int cols, int rows) {
            this.type = type;
            this.data = data;
            this.cols = cols;
            this.rows = rows;
        }
    }

    public static class CommandResult {
        public String output;
        public String error;
        public int exitCode;

        public CommandResult(String output, String error, int exitCode) {
            this.output = output;
            this.error = error;
            this.exitCode = exitCode;
        }
    }
}

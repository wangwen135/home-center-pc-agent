package com.wwh.home.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wwh.home.service.dto.AgentIdentity;
import com.wwh.home.service.dto.CmdResult;
import com.wwh.home.util.ConfigUtil;
import com.wwh.home.util.SystemUtil;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.UUID;

public class AgentWebSocketClient extends WebSocketClient {
    private final AgentIdentity identity;
    private final CommandExecutionService commandExecutionService;

    public AgentWebSocketClient(URI serverUri, AgentIdentity identity) {
        super(serverUri);
        this.identity = identity;
        this.commandExecutionService = new CommandExecutionService();
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        System.out.println("WebSocket connected: " + getURI());
        sendRegister();
    }

    @Override
    public void onMessage(String message) {
        try {
            JSONObject payload = JSONObject.parseObject(message);
            String type = payload.getString("type");
            if ("COMMAND".equals(type)) {
                handleCommand(payload);
            } else if ("EXEC".equals(type)) {
                handleExec(payload);
            } else if ("PING".equals(type)) {
                sendPong(payload.getString("requestId"));
            } else if ("REGISTERED".equals(type)) {
                System.out.println("Agent registered, deviceId=" + payload.getString("deviceId"));
            } else {
                System.out.println("Unsupported WebSocket message type: " + type);
            }
        } catch (Exception e) {
            System.err.println("Failed to handle WebSocket message: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket closed: code=" + code + ", reason=" + reason + ", remote=" + remote);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
    }

    private void sendRegister() {
        JSONObject payload = new JSONObject();
        payload.put("type", "REGISTER");
        payload.put("requestId", UUID.randomUUID().toString());
        payload.put("agentId", identity.getAgentId());
        payload.put("name", identity.getName());
        payload.put("hostname", identity.getHostname());
        payload.put("macAddresses", new JSONArray(identity.getMacAddresses()));
        payload.put("agentVersion", identity.getAgentVersion());
        payload.put("osName", identity.getOsName());
        send(payload.toJSONString());
    }

    private void handleCommand(JSONObject payload) {
        String requestId = payload.getString("requestId");
        String command = payload.getString("command");
        JSONObject result = baseResult(requestId);

        try {
            if ("shutdown".equalsIgnoreCase(command)) {
                SystemUtil.shutdownSystem();
                result.put("success", true);
                result.put("message", "shutdown command accepted");
            } else if ("screenshot".equalsIgnoreCase(command)) {
                String uploadResult = ScreenshotService.takeAndUpload(identity.getAgentId());
                boolean success = uploadResult != null && uploadResult.startsWith("OK:");
                result.put("success", success);
                result.put("message", uploadResult);
                if (!success) {
                    result.put("error", uploadResult);
                }
            } else if ("check-update".equalsIgnoreCase(command)) {
                String updateResult = UpdateService.checkAndUpdate();
                boolean success = updateResult != null && updateResult.startsWith("OK:");
                result.put("success", success);
                result.put("message", updateResult);
                if (!success) {
                    result.put("error", updateResult);
                }
            } else {
                result.put("success", false);
                result.put("error", "unsupported command: " + command);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        send(result.toJSONString());
    }

    private void handleExec(JSONObject payload) {
        String requestId = payload.getString("requestId");
        String command = payload.getString("command");
        int timeoutSeconds = payload.getIntValue("timeoutSeconds", ConfigUtil.getInt("cmd.default-timeout-seconds", 60));
        int maxOutputBytes = payload.getIntValue("maxOutputBytes", ConfigUtil.getInt("cmd.max-output-bytes", 1048576));

        JSONObject result = baseResult(requestId);
        if (!Boolean.parseBoolean(ConfigUtil.get("cmd.enabled", "true"))) {
            result.put("success", false);
            result.put("exitCode", -1);
            result.put("error", "command execution disabled");
            send(result.toJSONString());
            return;
        }

        CmdResult cmdResult = commandExecutionService.execute(command, timeoutSeconds, maxOutputBytes);
        result.put("success", cmdResult.isSuccess());
        result.put("exitCode", cmdResult.getExitCode());
        result.put("stdout", cmdResult.getStdout());
        result.put("stderr", cmdResult.getStderr());
        result.put("timedOut", cmdResult.isTimedOut());
        result.put("truncated", cmdResult.isTruncated());
        result.put("durationMillis", cmdResult.getDurationMillis());
        result.put("error", cmdResult.getError());
        send(result.toJSONString());
    }

    private void sendPong(String requestId) {
        JSONObject payload = new JSONObject();
        payload.put("type", "PONG");
        payload.put("requestId", requestId);
        payload.put("agentId", identity.getAgentId());
        send(payload.toJSONString());
    }

    private JSONObject baseResult(String requestId) {
        JSONObject result = new JSONObject();
        result.put("type", "RESULT");
        result.put("requestId", requestId);
        result.put("agentId", identity.getAgentId());
        return result;
    }
}

package com.wwh.home.service;

import com.wwh.home.service.dto.AgentIdentity;
import com.wwh.home.util.ConfigUtil;

import java.net.URI;
import java.net.URISyntaxException;

public class AgentWebSocketService {
    private static final String ENDPOINT = "/api/agent/ws";

    private final String serverUrl;
    private final AgentIdentity identity;

    public AgentWebSocketService(String serverUrl, AgentIdentity identity) {
        this.serverUrl = serverUrl;
        this.identity = identity;
    }

    public void startForever() {
        URI uri;
        try {
            uri = toWebSocketUri(serverUrl);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid server.url: " + serverUrl, e);
        }

        int reconnectSeconds = ConfigUtil.getInt("agent.reconnect.seconds", 5);
        while (true) {
            AgentWebSocketClient client = new AgentWebSocketClient(uri, identity);
            try {
                System.out.println("Connecting WebSocket: " + uri);
                client.connectBlocking();
                while (client.isOpen()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                client.close();
                return;
            } catch (Exception e) {
                System.err.println("WebSocket connection failed: " + e.getMessage());
            }

            sleepBeforeReconnect(reconnectSeconds);
        }
    }

    public static URI toWebSocketUri(String serverUrl) throws URISyntaxException {
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            throw new URISyntaxException("", "server.url is empty");
        }

        String trimmed = serverUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        URI base = new URI(trimmed);
        String scheme = base.getScheme();
        if ("http".equalsIgnoreCase(scheme)) {
            scheme = "ws";
        } else if ("https".equalsIgnoreCase(scheme)) {
            scheme = "wss";
        } else if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            throw new URISyntaxException(serverUrl, "server.url scheme must be http, https, ws, or wss");
        }

        String path = base.getPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            path = ENDPOINT;
        } else if (!path.endsWith(ENDPOINT)) {
            path = path + ENDPOINT;
        }

        return new URI(scheme, base.getUserInfo(), base.getHost(), base.getPort(), path, base.getQuery(), base.getFragment());
    }

    private void sleepBeforeReconnect(int reconnectSeconds) {
        try {
            Thread.sleep(Math.max(reconnectSeconds, 1) * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

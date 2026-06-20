package com.wwh.home.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentWebSocketServiceTest {

    @Test
    void convertsHttpServerUrlToWebSocketEndpoint() throws Exception {
        assertEquals("ws://127.0.0.1:8867/api/agent/ws",
                AgentWebSocketService.toWebSocketUri("http://127.0.0.1:8867").toString());
    }

    @Test
    void convertsHttpsServerUrlToSecureWebSocketEndpoint() throws Exception {
        assertEquals("wss://example.com/api/agent/ws",
                AgentWebSocketService.toWebSocketUri("https://example.com/").toString());
    }

    @Test
    void keepsExplicitWebSocketEndpoint() throws Exception {
        assertEquals("ws://localhost:8867/api/agent/ws",
                AgentWebSocketService.toWebSocketUri("ws://localhost:8867/api/agent/ws").toString());
    }

    @Test
    void rejectsUnsupportedScheme() {
        assertThrows(Exception.class, () -> AgentWebSocketService.toWebSocketUri("tcp://localhost:8867"));
    }
}

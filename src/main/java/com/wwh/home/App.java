package com.wwh.home;

import com.wwh.home.service.AgentIdentityService;
import com.wwh.home.service.AgentWebSocketService;
import com.wwh.home.service.dto.AgentIdentity;
import com.wwh.home.util.ConfigUtil;

/**
 * home-center PC agent.
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Start home-center-pc-agent ...");

        String serverUrl = ConfigUtil.get("server.url", "http://192.168.31.88:8866");
        String checkInterval = ConfigUtil.get("check.interval.hours", "24");
        AgentIdentity identity = new AgentIdentityService().loadOrCreate();

        System.out.println("config - server.url: " + serverUrl);
        System.out.println("config - check.interval.hours: " + checkInterval);
        System.out.println("agent - id: " + identity.getAgentId());
        System.out.println("agent - name: " + identity.getName());

        new AgentWebSocketService(serverUrl, identity).startForever();
    }
}

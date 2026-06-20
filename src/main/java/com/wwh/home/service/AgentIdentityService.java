package com.wwh.home.service;

import com.wwh.home.service.dto.AgentIdentity;
import com.wwh.home.util.ConfigUtil;
import com.wwh.home.util.MacAddressUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class AgentIdentityService {
    private static final String STATE_FILE_NAME = "pc-agent.state";
    private static final String AGENT_ID_KEY = "agent.id";
    private static final String VERSION = "1.0";

    public AgentIdentity loadOrCreate() {
        String agentId = loadAgentId();
        if (agentId == null || agentId.trim().isEmpty()) {
            agentId = UUID.randomUUID().toString();
            saveAgentId(agentId);
        }

        String hostname = resolveHostname();
        String name = ConfigUtil.get("agent.name", hostname);
        List<String> macAddresses = MacAddressUtil.getAllMacAddress();
        String osName = System.getProperty("os.name", "") + " " + System.getProperty("os.version", "");
        return new AgentIdentity(agentId, name, hostname, macAddresses, osName.trim(), VERSION);
    }

    private String loadAgentId() {
        File stateFile = getStateFile();
        if (!stateFile.exists()) {
            return null;
        }

        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(stateFile), "UTF-8"))) {
            properties.load(reader);
            return properties.getProperty(AGENT_ID_KEY);
        } catch (Exception e) {
            System.err.println("Failed to load agent state: " + e.getMessage());
            return null;
        }
    }

    private void saveAgentId(String agentId) {
        File stateFile = getStateFile();
        File parent = stateFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            System.err.println("Failed to create agent state dir: " + parent.getAbsolutePath());
            return;
        }

        Properties properties = new Properties();
        properties.setProperty(AGENT_ID_KEY, agentId);
        try (FileOutputStream outputStream = new FileOutputStream(stateFile)) {
            properties.store(outputStream, "home-center-pc-agent local state");
        } catch (Exception e) {
            System.err.println("Failed to save agent state: " + e.getMessage());
        }
    }

    private File getStateFile() {
        return new File(ConfigUtil.getConfigDir(), STATE_FILE_NAME);
    }

    private String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }
}

package com.wwh.home.service.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AgentIdentity {
    private final String agentId;
    private final String name;
    private final String hostname;
    private final List<String> macAddresses;
    private final String osName;
    private final String agentVersion;

    public AgentIdentity(String agentId, String name, String hostname, List<String> macAddresses,
                         String osName, String agentVersion) {
        this.agentId = agentId;
        this.name = name;
        this.hostname = hostname;
        this.macAddresses = Collections.unmodifiableList(new ArrayList<String>(macAddresses));
        this.osName = osName;
        this.agentVersion = agentVersion;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getName() {
        return name;
    }

    public String getHostname() {
        return hostname;
    }

    public List<String> getMacAddresses() {
        return macAddresses;
    }

    public String getOsName() {
        return osName;
    }

    public String getAgentVersion() {
        return agentVersion;
    }
}

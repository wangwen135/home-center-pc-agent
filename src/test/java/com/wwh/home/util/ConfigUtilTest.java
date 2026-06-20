package com.wwh.home.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigUtilTest {

    @AfterEach
    void cleanupConfigFile() throws Exception {
        Files.deleteIfExists(configPath());
        ConfigUtil.reload();
    }

    @Test
    void readsUtf8KeyValuesWhenConfigFileExists() throws Exception {
        Files.createDirectories(configPath().getParent());
        Files.write(configPath(), "server.url=http://127.0.0.1:8867\ncmd.enabled=false\n".getBytes(StandardCharsets.UTF_8));

        ConfigUtil.reload();

        assertEquals("http://127.0.0.1:8867", ConfigUtil.get("server.url", "http://localhost"));
        assertEquals("false", ConfigUtil.get("cmd.enabled", "true"));
    }

    @Test
    void returnsDefaultValueWhenKeyIsMissing() throws Exception {
        Files.deleteIfExists(configPath());

        ConfigUtil.reload();

        assertEquals("default-value", ConfigUtil.get("missing.key", "default-value"));
        assertEquals("", ConfigUtil.get("missing.key"));
    }

    @Test
    void reloadClearsOldValuesAfterConfigFileIsDeleted() throws Exception {
        Files.createDirectories(configPath().getParent());
        Files.write(configPath(), "server.url=http://old-server\n".getBytes(StandardCharsets.UTF_8));
        ConfigUtil.reload();

        Files.deleteIfExists(configPath());
        ConfigUtil.reload();

        assertEquals("http://new-server", ConfigUtil.get("server.url", "http://new-server"));
    }

    private static Path configPath() throws URISyntaxException {
        String path = ConfigUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        File file = new File(path);
        File dir = file.isFile() ? file.getParentFile() : file;
        return dir.toPath().resolve("pc-agent.conf");
    }
}

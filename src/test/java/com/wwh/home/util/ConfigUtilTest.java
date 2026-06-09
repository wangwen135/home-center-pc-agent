package com.wwh.home.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigUtilTest {

    @AfterEach
    void 清理配置文件() throws Exception {
        Files.deleteIfExists(configPath());
        ConfigUtil.reload();
    }

    @Test
    @DisplayName("配置读取：文件存在时读取 UTF-8 key value")
    void 配置读取_文件存在时读取值() throws Exception {
        Files.createDirectories(configPath().getParent());
        Files.write(configPath(), "access.token=测试-token\ncmd.enabled=false\n".getBytes(StandardCharsets.UTF_8));

        ConfigUtil.reload();

        assertEquals("测试-token", ConfigUtil.get("access.token", "changeme"));
        assertEquals("false", ConfigUtil.get("cmd.enabled", "true"));
    }

    @Test
    @DisplayName("配置读取：缺失 key 返回默认值")
    void 配置读取_缺失Key返回默认值() throws Exception {
        Files.deleteIfExists(configPath());

        ConfigUtil.reload();

        assertEquals("默认值", ConfigUtil.get("missing.key", "默认值"));
        assertEquals("", ConfigUtil.get("missing.key"));
    }

    @Test
    @DisplayName("配置读取：文件删除后 reload 清空旧值")
    void 配置读取_文件删除后Reload清空旧值() throws Exception {
        Files.createDirectories(configPath().getParent());
        Files.write(configPath(), "access.token=old\n".getBytes(StandardCharsets.UTF_8));
        ConfigUtil.reload();

        Files.deleteIfExists(configPath());
        ConfigUtil.reload();

        assertEquals("new-default", ConfigUtil.get("access.token", "new-default"));
    }

    private static Path configPath() throws URISyntaxException {
        String path = ConfigUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        File file = new File(path);
        File dir = file.isFile() ? file.getParentFile() : file;
        return dir.toPath().resolve("pc-agent.conf");
    }
}

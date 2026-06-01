package com.wwh.home.util;

import java.io.*;
import java.util.Properties;

/**
 * 配置文件工具类
 * 读取 jar 同级目录下的 pc-agent.conf（key=value 格式）
 *
 * @author wangwh
 */
public class ConfigUtil {

    private static final String CONFIG_FILE_NAME = "pc-agent.conf";
    private static Properties properties = new Properties();

    static {
        loadConfig();
    }

    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        String jarPath = getJarDir();
        File configFile = new File(jarPath, CONFIG_FILE_NAME);

        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), "UTF-8"))) {
                properties.load(reader);
                System.out.println("配置文件加载成功: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("加载配置文件失败: " + e.getMessage());
            }
        } else {
            System.out.println("配置文件不存在，使用默认配置: " + configFile.getAbsolutePath());
        }
    }

    /**
     * 获取 jar 所在目录
     */
    private static String getJarDir() {
        try {
            String path = ConfigUtil.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File file = new File(path);
            if (file.isFile()) {
                return file.getParent();
            }
            return file.getPath();
        } catch (Exception e) {
            System.err.println("获取 jar 目录失败: " + e.getMessage());
            return ".";
        }
    }

    /**
     * 获取配置值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 获取配置值，默认为空字符串
     *
     * @param key 配置键
     * @return 配置值
     */
    public static String get(String key) {
        return get(key, "");
    }

    /**
     * 重新加载配置文件
     */
    public static void reload() {
        properties.clear();
        loadConfig();
    }
}

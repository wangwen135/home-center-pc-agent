package com.wwh.home.service;

import com.wwh.home.util.ConfigUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 自动更新服务
 * 检查版本、下载 jar、替换并重启
 *
 * @author wangwh
 */
public class UpdateService {

    private static final String CURRENT_VERSION = "1.0";

    /**
     * 检查更新并执行更新
     *
     * @return 更新结果
     */
    public static String checkAndUpdate() {
        try {
            String serverUrl = ConfigUtil.get("server.url", "http://192.168.31.88:8866");
            String versionUrl = serverUrl + "/api/version";

            // 1. 检查版本
            System.out.println("正在检查更新: " + versionUrl);
            String versionJson = httpGet(versionUrl);
            if (versionJson == null || versionJson.isEmpty()) {
                return "ERROR: 无法获取版本信息";
            }

            System.out.println("版本信息: " + versionJson);

            // 简单解析 JSON
            String remoteVersion = extractJsonValue(versionJson, "version");
            String downloadUrl = extractJsonValue(versionJson, "downloadUrl");

            if (remoteVersion == null || remoteVersion.isEmpty()) {
                return "ERROR: 版本信息解析失败";
            }

            if (remoteVersion.equals(CURRENT_VERSION)) {
                return "OK: 已是最新版本 " + CURRENT_VERSION;
            }

            System.out.println("发现新版本: " + remoteVersion + "（当前: " + CURRENT_VERSION + "）");

            // 2. 下载新 jar
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                return "ERROR: 下载地址为空";
            }

            // 如果 downloadUrl 是相对路径，拼接 serverUrl
            if (!downloadUrl.startsWith("http")) {
                downloadUrl = serverUrl + downloadUrl;
            }

            File tempJar = downloadJar(downloadUrl);
            if (tempJar == null) {
                return "ERROR: 下载更新失败";
            }

            System.out.println("新版本下载完成: " + tempJar.getAbsolutePath());

            // 3. 替换并重启
            return replaceAndRestart(tempJar);

        } catch (Exception e) {
            System.err.println("检查更新异常: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * HTTP GET 请求
     */
    private static String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            } else {
                System.err.println("HTTP GET 失败: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            System.err.println("HTTP GET 异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 下载 jar 到临时目录
     */
    private static File downloadJar(String downloadUrl) {
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(120000);

            File tempDir = new File(System.getProperty("java.io.tmpdir"), "home-center-update");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            String fileName = "update-" + System.currentTimeMillis() + ".jar";
            File tempFile = new File(tempDir, fileName);

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            }

            System.out.println("下载完成: " + tempFile.getAbsolutePath() + " (" + tempFile.length() + " bytes)");
            return tempFile;
        } catch (Exception e) {
            System.err.println("下载 jar 异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 替换旧 jar 并重启进程
     */
    private static String replaceAndRestart(File newJar) {
        try {
            // 获取当前 jar 路径
            String currentJarPath = UpdateService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File currentJar = new File(currentJarPath);

            if (!currentJar.exists()) {
                return "ERROR: 找不到当前 jar 文件: " + currentJarPath;
            }

            String os = System.getProperty("os.name");
            File finalJar;

            if (os.contains("Win")) {
                // Windows 下不能覆盖运行中的 jar，先复制到临时位置
                File backupDir = new File(System.getProperty("java.io.tmpdir"), "home-center-backup");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }
                File backupJar = new File(backupDir, "old-" + System.currentTimeMillis() + ".jar");
                Files.copy(currentJar.toPath(), backupJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Windows 环境：旧 jar 已备份到 " + backupJar.getAbsolutePath());

                // 创建一个 bat 脚本来延迟替换
                finalJar = currentJar;
                String batPath = new File(backupDir, "update.bat").getAbsolutePath();
                try (PrintWriter writer = new PrintWriter(new FileWriter(batPath))) {
                    writer.println("@echo off");
                    writer.println("timeout /t 3 /nobreak >nul");
                    writer.println("copy /Y \"" + newJar.getAbsolutePath() + "\" \"" + currentJar.getAbsolutePath() + "\"");
                    writer.println("del \"" + newJar.getAbsolutePath() + "\"");
                    writer.println("start java -jar \"" + currentJar.getAbsolutePath() + "\"");
                    writer.println("del \"%~f0\"");
                }

                // 执行 bat
                Runtime.getRuntime().exec("cmd /c start \"\" \"" + batPath + "\"");
            } else {
                // Linux/Mac：直接覆盖并重启
                Files.copy(newJar.toPath(), currentJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                finalJar = currentJar;
                newJar.delete();
            }

            // 启动新进程
            String javaCmd = "java";
            String jarPath = finalJar.getAbsolutePath();
            System.out.println("正在启动新进程: " + javaCmd + " -jar " + jarPath);
            Runtime.getRuntime().exec(new String[]{javaCmd, "-jar", jarPath});

            // 延迟退出，确保新进程启动
            Thread.sleep(2000);
            System.out.println("更新完成，正在退出旧进程...");
            System.exit(0);

            return "OK: 更新完成，正在重启";
        } catch (Exception e) {
            System.err.println("替换重启异常: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * 简单的 JSON 值提取（不依赖外部 JSON 库）
     */
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(":", keyIndex + searchKey.length());
        if (colonIndex < 0) {
            return null;
        }

        int valueStart = -1;
        int valueEnd = -1;

        // 查找值的起始位置（跳过冒号后的空白）
        for (int i = colonIndex + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                valueStart = i + 1;
                valueEnd = json.indexOf('"', valueStart);
                break;
            } else if (c != ' ' && c != '\t') {
                // 非字符串值
                valueStart = i;
                for (int j = i; j < json.length(); j++) {
                    char cc = json.charAt(j);
                    if (cc == ',' || cc == '}' || cc == ']') {
                        valueEnd = j;
                        break;
                    }
                }
                break;
            }
        }

        if (valueStart >= 0 && valueEnd > valueStart) {
            return json.substring(valueStart, valueEnd).trim();
        }
        return null;
    }
}

package com.wwh.home.service.handler;

import com.wwh.home.service.CommandExecutionService;
import com.wwh.home.service.ScreenshotService;
import com.wwh.home.service.UpdateService;
import com.wwh.home.service.dto.CmdRequest;
import com.wwh.home.service.dto.CmdResult;
import com.wwh.home.service.protocol.AgentProtocol;
import com.wwh.home.util.ConfigUtil;
import com.wwh.home.util.SystemUtil;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 处理指令单线程
 * 支持 AUTH 认证，新增 screenshot 和 check-update 命令
 *
 * @author wangwh
 * @date 2024/06/03
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private final CommandExecutionService commandExecutionService = new CommandExecutionService();

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        try (BufferedInputStream in = new BufferedInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            // AUTH 认证：第一条消息必须是 "AUTH <token>"
            String firstLine = AgentProtocol.readLine(in);
            if (firstLine == null) {
                System.err.println("客户端未发送任何消息即断开: " + clientSocket.getRemoteSocketAddress());
                return;
            }
            System.out.println("收到消息: " + firstLine);

            String authToken = ConfigUtil.get("access.token", "changeme");
            if (!firstLine.equals("AUTH " + authToken)) {
                AgentProtocol.writeLine(out, "AUTH FAILED");
                System.err.println("认证失败: " + clientSocket.getRemoteSocketAddress());
                return;
            }

            AgentProtocol.writeLine(out, "AUTH OK");
            System.out.println("认证成功: " + clientSocket.getRemoteSocketAddress());

            // 处理后续命令
            String inputLine;
            while ((inputLine = AgentProtocol.readLine(in)) != null) {
                System.out.println("收到消息: " + inputLine);
                if (inputLine.startsWith("CMD ")) {
                    processCmdFrame(inputLine, in, out);
                } else {
                    String response = processCommand(inputLine);
                    AgentProtocol.writeLine(out, response);
                    System.out.println("返回响应: " + response);
                }
            }
        } catch (IOException e) {
            System.err.println("处理连接时发生异常：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("关闭连接：" + clientSocket.getRemoteSocketAddress());
            } catch (IOException e) {
                System.err.println("关闭连接时发生异常：" + e.getMessage());
            }
        }
    }

    private String processCommand(String command) {
        switch (command.toLowerCase()) {
            case "hello":
                return "Hello, Client!";
            case "time":
                return "time: " + new java.util.Date().toString();
            case "shutdown":
                SystemUtil.shutdownSystem();
                return "Copy that!";
            case "screenshot":
                return ScreenshotService.takeAndUpload();
            case "check-update":
                return UpdateService.checkAndUpdate();
            default:
                return "未知指令: " + command;
        }
    }

    private void processCmdFrame(String headerLine, BufferedInputStream in, DataOutputStream out) throws IOException {
        AgentProtocol.FrameHeader header = parseCmdHeader(headerLine);
        if (!"CMD".equals(header.getType())) {
            CmdResult result = errorResult("不支持的帧类型: " + header.getType());
            writeCmdResult(out, result);
            return;
        }

        byte[] payload = AgentProtocol.readFramePayload(in, header.getLength());
        String json = new String(payload, StandardCharsets.UTF_8);
        System.out.println("收到 CMD payload: " + json);

        CmdResult result;
        if (!getBooleanConfig("cmd.enabled", true)) {
            result = errorResult("CMD disabled");
        } else {
            int defaultTimeoutSeconds = getIntConfig("cmd.default-timeout-seconds", 60);
            int maxOutputBytes = getIntConfig("cmd.max-output-bytes", 1048576);
            CmdRequest request = CmdRequest.fromJson(json, defaultTimeoutSeconds);
            int timeoutSeconds = request.getTimeoutSeconds() > 0 ? request.getTimeoutSeconds() : defaultTimeoutSeconds;
            result = commandExecutionService.execute(request.getCommand(), timeoutSeconds, maxOutputBytes);
        }

        writeCmdResult(out, result);
    }

    private AgentProtocol.FrameHeader parseCmdHeader(String headerLine) throws IOException {
        String[] parts = headerLine.split("\\s+", 2);
        if (parts.length != 2) {
            throw new IOException("非法 CMD 帧头: " + headerLine);
        }

        int length;
        try {
            length = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IOException("非法 CMD payload 长度: " + headerLine, e);
        }

        if (length < 0) {
            throw new IOException("CMD payload 长度不能为负数: " + headerLine);
        }
        return new AgentProtocol.FrameHeader(parts[0], length);
    }

    private void writeCmdResult(DataOutputStream out, CmdResult result) throws IOException {
        byte[] resultPayload = result.toJson().getBytes(StandardCharsets.UTF_8);
        AgentProtocol.writeFrame(out, "CMD-RESULT", resultPayload);
        System.out.println("返回 CMD 结果: " + result.toJson());
    }

    private CmdResult errorResult(String message) {
        CmdResult result = new CmdResult();
        result.setSuccess(false);
        result.setExitCode(-1);
        result.setError(message);
        return result;
    }

    private boolean getBooleanConfig(String key, boolean defaultValue) {
        return Boolean.parseBoolean(ConfigUtil.get(key, String.valueOf(defaultValue)));
    }

    private int getIntConfig(String key, int defaultValue) {
        try {
            return Integer.parseInt(ConfigUtil.get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

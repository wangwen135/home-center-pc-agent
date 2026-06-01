package com.wwh.home.service.handler;

import com.wwh.home.service.ScreenshotService;
import com.wwh.home.service.UpdateService;
import com.wwh.home.util.ConfigUtil;
import com.wwh.home.util.SystemUtil;

import java.io.*;
import java.net.Socket;

/**
 * 处理指令单线程
 * 支持 AUTH 认证，新增 screenshot 和 check-update 命令
 *
 * @author wangwh
 * @date 2024/06/03
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true)) {

            // AUTH 认证：第一条消息必须是 "AUTH <token>"
            String firstLine = in.readLine();
            if (firstLine == null) {
                System.err.println("客户端未发送任何消息即断开: " + clientSocket.getRemoteSocketAddress());
                return;
            }
            System.out.println("收到消息: " + firstLine);

            String authToken = ConfigUtil.get("access.token", "changeme");
            if (!firstLine.equals("AUTH " + authToken)) {
                out.println("AUTH FAILED");
                System.err.println("认证失败: " + clientSocket.getRemoteSocketAddress());
                return;
            }

            out.println("AUTH OK");
            System.out.println("认证成功: " + clientSocket.getRemoteSocketAddress());

            // 处理后续命令
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("收到消息: " + inputLine);
                String response = processCommand(inputLine);
                out.println(response);
                System.out.println("返回响应: " + response);
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
}

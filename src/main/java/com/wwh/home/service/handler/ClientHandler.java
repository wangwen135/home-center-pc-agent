package com.wwh.home.service.handler;

import com.wwh.home.util.SystemUtil;

import java.io.*;
import java.net.Socket;

/**
 * 处理指令单线程
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
        // 处理不同的指令
        switch (command.toLowerCase()) {
            case "hello":
                return "Hello, Client!";
            case "time":
                return "time: " + new java.util.Date().toString();
            case "shutdown":
                SystemUtil.shutdownSystem();
                return "Copy that!";
            default:
                return "未知指令: " + command;
        }
    }
}

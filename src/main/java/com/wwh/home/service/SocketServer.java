package com.wwh.home.service;

import com.wwh.home.service.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Socket 服务端，用于接收指令
 *
 * @author wangwh
 * @date 2024/06/03
 */
public class SocketServer {

    public static int PORT = 65432;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("监听启动，在端口：" + PORT + "，等待连接...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("已连接：" + clientSocket.getRemoteSocketAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("发生异常：" + e.getMessage());
        }
    }
}

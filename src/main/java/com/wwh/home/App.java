package com.wwh.home;

import com.wwh.home.service.SocketServer;
import com.wwh.home.service.WolListener;
import com.wwh.home.util.ConfigUtil;

/**
 * home-center pc端代理程序
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Start home-center-pc-agent ...");

        // 加载配置文件
        String serverUrl = ConfigUtil.get("server.url", "http://192.168.31.88:8866");
        String accessToken = ConfigUtil.get("access.token", "changeme");
        String checkInterval = ConfigUtil.get("check.interval.hours", "24");
        System.out.println("配置 - server.url: " + serverUrl);
        System.out.println("配置 - check.interval.hours: " + checkInterval);

        Thread wolListenerThread = new Thread(new WolListener());
        wolListenerThread.setDaemon(true);
        wolListenerThread.start();

        Thread.yield();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("");
        System.out.println("-------------------------------");
        SocketServer socketServer = new SocketServer();
        socketServer.start();

        System.out.println("stop");
    }
}

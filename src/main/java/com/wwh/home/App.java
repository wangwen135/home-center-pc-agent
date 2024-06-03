package com.wwh.home;

import com.wwh.home.service.SocketServer;
import com.wwh.home.service.WolListener;

/**
 * home-center pc端代理程序
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Start home-center-pc-agent ...");

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

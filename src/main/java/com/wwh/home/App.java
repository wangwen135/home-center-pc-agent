package com.wwh.home;

import com.wwh.home.service.WolListener;

/**
 * home-center pc端代理程序
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Start home-center-pc-agent ...");

        Thread wolListenerThread = new Thread(new WolListener());
//        wolListenerThread.setDaemon(true);
        wolListenerThread.start();


    }
}

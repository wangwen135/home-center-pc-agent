package com.wwh.home.service;

import com.wwh.home.util.MacAddressUtil;
import com.wwh.home.util.SystemUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 监听wol数据包 <br>
 * 用于实现关机功能
 *
 * @author wangwh
 * @date 2024/05/31
 */
public class WolListener implements Runnable {
    //WOL通常使用端口9
    public static final int PORT = 9;
    //数据包大小
    private static final int BUFFER_SIZE = 102;

    private static final int RESET_TIME = 3000; // 3秒重置计数器，一个包和一个包之间不能间隔超过3秒
    private static final int PACKET_THRESHOLD = 8;

    private static Timer timer = new Timer();

    private static String lastMac;
    private static int packetCount = 0;

    @Override
    public void run() {

        List<String> macList = MacAddressUtil.getAllMacAddress();
        if (macList == null || macList.isEmpty()) {
            System.err.println("The mac address was not obtained,WolListener Startup failed!");
            return;
        }
        System.out.println("Local valid mac address:");
        macList.forEach(System.out::println);
        System.out.println("-------------------");

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("UDP WOL Listener started, listening on port " + PORT);

            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                System.out.println("Received packet ...");
                byte[] data = packet.getData();
                if (isWOLPacket(data)) {
                    String macAddress = extractMACAddress(data);
                    System.out.println("The mac address is: " + macAddress);

                    //与上一次的MAC一致
                    if (macAddress.equals(lastMac)) {
                        packetCount++;
                        resetTimer();
                        if (packetCount >= PACKET_THRESHOLD) {
                            System.out.println("Received 8 WOL packets in short time. Shutting down system...");
                            SystemUtil.shutdownSystem();
                        } else {
                            System.out.println("Packet Count = " + packetCount);
                        }
                    } else {
                        clear();
                        if (macList.contains(macAddress)) {
                            lastMac = macAddress;
                            packetCount++;
                            resetTimer();
                        } else {
                            System.err.println("Mac address error! reject!");
                        }
                    }

                } else {
                    System.out.println("Received non-WOL packet");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 重置计时器
     */
    private static void resetTimer() {
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                clear();
            }
        }, RESET_TIME);
    }

    private static void clear() {
        System.out.println("clear ...");
        lastMac = null;
        packetCount = 0;
    }

    private static boolean isWOLPacket(byte[] data) {
        if (data.length < BUFFER_SIZE) {
            return false;
        }

        // 检查数据包前6个字节是否全为0xFF
        for (int i = 0; i < 6; i++) {
            if (data[i] != (byte) 0xFF) {
                return false;
            }
        }

        // 检查接下来的16个MAC地址
        for (int i = 6; i < BUFFER_SIZE; i += 6) {
            for (int j = 0; j < 6; j++) {
                if (data[i + j] != data[6 + j]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 提取wol包中的mac地址
     *
     * @param data
     * @return
     */
    private static String extractMACAddress(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 6; i < 12; i++) {
            sb.append(String.format("%02X", data[i]));
            if (i < 11) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

}
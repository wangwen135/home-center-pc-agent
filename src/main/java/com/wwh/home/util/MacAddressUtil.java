package com.wwh.home.util;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * MAC地址工具
 *
 * @author wangwh
 * @date 2024/05/31
 */
public class MacAddressUtil {

    public static void main(String[] args) {
        String macAddress = getMacAddress();
        System.out.println("MAC地址：" + macAddress);

        List<String> list = getAllMacAddress();
        System.out.println("全部的MAC地址:");
        list.forEach(System.out::println);
    }

    /**
     * <pre>
     * 返回：网络接口的MAC地址
     * 跳过：回环接口、虚拟接口、未启用的接口，以及没有IP地址的接口。
     * </pre>
     *
     * @return
     */
    public static List<String> getAllMacAddress() {
        List<String> list = new ArrayList<>();
        try {
            // 获取本地主机的网络接口列表
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // 排除回环接口、虚拟接口和没有IP地址的接口
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp() || networkInterface.getInterfaceAddresses().isEmpty()) {
                    continue;
                }

                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    // 将MAC地址转换为十六进制字符串
                    StringBuilder macAddressBuilder = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        macAddressBuilder.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    list.add(macAddressBuilder.toString());
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 返回第一个有效的MAC地址
     *
     * @return
     */
    public static String getMacAddress() {
        List<String> list = getAllMacAddress();
        if (list == null || list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }
}

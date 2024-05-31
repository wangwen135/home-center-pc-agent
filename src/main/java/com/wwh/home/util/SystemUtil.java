package com.wwh.home.util;

/**
 * windows系统工具
 *
 * @author wangwh
 * @date 2024/05/31
 */
public class SystemUtil {
    public static void rebootSystem() {
        try {
            String shutdownCommand;
            String os = System.getProperty("os.name");

            if (os.contains("Win")) {
                shutdownCommand = "shutdown -r -t 0";
            } else if (os.contains("Mac")) {
                shutdownCommand = "sudo shutdown -r now";
            } else if (os.contains("Linux")) {
                shutdownCommand = "sudo reboot";
            } else {
                throw new UnsupportedOperationException("Unsupported operating system.");
            }

            Runtime.getRuntime().exec(shutdownCommand);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shutdownSystem() {
        try {
            String shutdownCommand;
            String os = System.getProperty("os.name");

            if (os.contains("Win")) {
//                shutdownCommand = "shutdown -s -t 0";
                shutdownCommand = "shutdown -s -t 60"; //60秒后关机
            } else if (os.contains("Mac")) {
                shutdownCommand = "sudo shutdown -h now";
            } else if (os.contains("Linux")) {
                shutdownCommand = "sudo shutdown -h now";
            } else {
                throw new UnsupportedOperationException("Unsupported operating system.");
            }

            Runtime.getRuntime().exec(shutdownCommand);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

package com.wwh.home.service;

import com.wwh.home.util.ConfigUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;

/**
 * 屏幕截图服务
 * 使用 java.awt.Robot 截屏，上传到服务器
 *
 * @author wangwh
 */
public class ScreenshotService {

    /**
     * 截取屏幕并返回 PNG 字节数组
     *
     * @return PNG 格式的字节数组
     */
    public static byte[] captureScreen() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenImage = robot.createScreenCapture(screenRect);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screenImage, "png", baos);
            baos.flush();
            byte[] imageBytes = baos.toByteArray();
            baos.close();

            System.out.println("截屏成功，图片大小: " + imageBytes.length + " bytes");
            return imageBytes;
        } catch (AWTException e) {
            System.err.println("截屏失败（AWTException）: " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.err.println("截屏失败（IOException）: " + e.getMessage());
            return null;
        }
    }

    /**
     * 上传截图到服务器
     *
     * @param imageBytes PNG 图片字节数组
     * @return 上传结果
     */
    public static String uploadScreenshot(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return "ERROR: 截图数据为空";
        }

        String serverUrl = ConfigUtil.get("server.url", "http://192.168.31.88:8866");
        String uploadUrl = serverUrl + "/api/screenshot";

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            String fileName = "screenshot-" + timestamp + ".png";
            String boundary = "----Boundary" + System.currentTimeMillis();

            URL url = new URL(uploadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            try (OutputStream os = connection.getOutputStream()) {
                // 写入 multipart header
                String header = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                        + "Content-Type: image/png\r\n\r\n";
                os.write(header.getBytes("UTF-8"));
                os.write(imageBytes);
                String footer = "\r\n--" + boundary + "--\r\n";
                os.write(footer.getBytes("UTF-8"));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection);

            if (responseCode == 200) {
                System.out.println("截图上传成功: " + responseBody);
                return "OK: " + responseBody;
            } else {
                System.err.println("截图上传失败，HTTP " + responseCode + ": " + responseBody);
                return "ERROR: HTTP " + responseCode + " - " + responseBody;
            }
        } catch (Exception e) {
            System.err.println("截图上传异常: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * 读取 HTTP 响应内容
     */
    private static String readResponse(HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            } catch (IOException ex) {
                return e.getMessage();
            }
        }
    }

    /**
     * 截屏并上传（一步完成）
     *
     * @return 上传结果
     */
    public static String takeAndUpload() {
        byte[] imageBytes = captureScreen();
        return uploadScreenshot(imageBytes);
    }
}

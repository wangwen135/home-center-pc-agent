package com.wwh.home.service;

import com.wwh.home.util.ConfigUtil;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotService {

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

            System.out.println("Screenshot captured: " + imageBytes.length + " bytes");
            return imageBytes;
        } catch (AWTException e) {
            System.err.println("Screenshot failed: " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.err.println("Screenshot failed: " + e.getMessage());
            return null;
        }
    }

    public static String uploadScreenshot(byte[] imageBytes) {
        return uploadScreenshot(imageBytes, null);
    }

    public static String uploadScreenshot(byte[] imageBytes, String agentId) {
        if (imageBytes == null || imageBytes.length == 0) {
            return "ERROR: screenshot data is empty";
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
            if (agentId != null && !agentId.trim().isEmpty()) {
                connection.setRequestProperty("X-Agent-Id", agentId.trim());
            }
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            try (OutputStream os = connection.getOutputStream()) {
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
                System.out.println("Screenshot uploaded: " + responseBody);
                return "OK: " + responseBody;
            }
            System.err.println("Screenshot upload failed, HTTP " + responseCode + ": " + responseBody);
            return "ERROR: HTTP " + responseCode + " - " + responseBody;
        } catch (Exception e) {
            System.err.println("Screenshot upload error: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    private static String readResponse(HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            return readAll(reader);
        } catch (IOException e) {
            if (connection.getErrorStream() == null) {
                return e.getMessage();
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                return readAll(reader);
            } catch (IOException ex) {
                return e.getMessage();
            }
        }
    }

    private static String readAll(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    public static String takeAndUpload() {
        return uploadScreenshot(captureScreen());
    }

    public static String takeAndUpload(String agentId) {
        return uploadScreenshot(captureScreen(), agentId);
    }
}

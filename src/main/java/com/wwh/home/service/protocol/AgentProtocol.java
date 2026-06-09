package com.wwh.home.service.protocol;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Agent 长度前缀协议工具类
 */
public class AgentProtocol {

    public static class FrameHeader {
        private final String type;
        private final int length;

        public FrameHeader(String type, int length) {
            this.type = type;
            this.length = length;
        }

        public String getType() {
            return type;
        }

        public int getLength() {
            return length;
        }
    }

    public static void writeFrame(DataOutputStream out, String type, byte[] payload) throws IOException {
        out.write((type + " " + payload.length + "\n").getBytes(StandardCharsets.UTF_8));
        out.write(payload);
        out.flush();
    }

    public static FrameHeader readFrameHeader(BufferedInputStream in) throws IOException {
        String line = readLine(in);
        if (line == null) {
            return null;
        }

        String[] parts = line.split("\\s+", 2);
        if (parts.length != 2) {
            throw new IOException("非法帧头: " + line);
        }

        int length;
        try {
            length = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IOException("非法帧长度: " + line, e);
        }

        if (length < 0) {
            throw new IOException("帧长度不能为负数: " + line);
        }
        return new FrameHeader(parts[0], length);
    }

    public static byte[] readFramePayload(BufferedInputStream in, int length) throws IOException {
        byte[] payload = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(payload, offset, length - offset);
            if (read == -1) {
                throw new EOFException("读取 payload 时连接已关闭");
            }
            offset += read;
        }
        return payload;
    }

    public static String readLine(BufferedInputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (true) {
            int b = in.read();
            if (b == -1) {
                if (buffer.size() == 0) {
                    return null;
                }
                break;
            }
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                buffer.write(b);
            }
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    public static void writeLine(DataOutputStream out, String line) throws IOException {
        out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}

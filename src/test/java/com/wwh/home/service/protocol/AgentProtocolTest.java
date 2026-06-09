package com.wwh.home.service.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentProtocolTest {

    @Test
    @DisplayName("帧读写：支持 CMD/CMD-RESULT/AUTH/ACK 类型")
    void 帧读写_支持多种类型() throws Exception {
        assertFrameRoundTrip("CMD", "{\"command\":\"pwd\"}");
        assertFrameRoundTrip("CMD-RESULT", "{\"success\":true}");
        assertFrameRoundTrip("AUTH", "token");
        assertFrameRoundTrip("ACK", "ok");
    }

    @Test
    @DisplayName("帧读写：允许空 payload")
    void 帧读写_允许空Payload() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        AgentProtocol.writeFrame(new DataOutputStream(bytes), "ACK", new byte[0]);

        BufferedInputStream in = input(bytes.toByteArray());
        AgentProtocol.FrameHeader header = AgentProtocol.readFrameHeader(in);

        assertEquals("ACK", header.getType());
        assertEquals(0, header.getLength());
        assertArrayEquals(new byte[0], AgentProtocol.readFramePayload(in, 0));
    }

    @Test
    @DisplayName("帧读写：读取超长 payload")
    void 帧读写_读取超长Payload() throws Exception {
        byte[] payload = new byte[128 * 1024];
        Arrays.fill(payload, (byte) 'a');

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        AgentProtocol.writeFrame(new DataOutputStream(bytes), "CMD", payload);

        BufferedInputStream in = input(bytes.toByteArray());
        AgentProtocol.FrameHeader header = AgentProtocol.readFrameHeader(in);

        assertEquals("CMD", header.getType());
        assertEquals(payload.length, header.getLength());
        assertArrayEquals(payload, AgentProtocol.readFramePayload(in, header.getLength()));
    }

    @Test
    @DisplayName("畸形帧：空输入返回 null")
    void 畸形帧_空输入返回Null() throws Exception {
        assertNull(AgentProtocol.readFrameHeader(input(new byte[0])));
    }

    @Test
    @DisplayName("畸形帧：缺少长度时报错")
    void 畸形帧_缺少长度时报错() {
        IOException exception = assertThrows(IOException.class,
                () -> AgentProtocol.readFrameHeader(input("CMD\n")));

        assertEquals("非法帧头: CMD", exception.getMessage());
    }

    @Test
    @DisplayName("畸形帧：非数字长度时报错")
    void 畸形帧_非数字长度时报错() {
        IOException exception = assertThrows(IOException.class,
                () -> AgentProtocol.readFrameHeader(input("CMD abc\n")));

        assertEquals("非法帧长度: CMD abc", exception.getMessage());
    }

    @Test
    @DisplayName("畸形帧：负数长度时报错")
    void 畸形帧_负数长度时报错() {
        IOException exception = assertThrows(IOException.class,
                () -> AgentProtocol.readFrameHeader(input("CMD -1\n")));

        assertEquals("帧长度不能为负数: CMD -1", exception.getMessage());
    }

    @Test
    @DisplayName("畸形帧：payload 提前结束时报 EOF")
    void 畸形帧_payload提前结束时报Eof() {
        assertThrows(EOFException.class,
                () -> AgentProtocol.readFramePayload(input("abc"), 5));
    }

    @Test
    @DisplayName("行协议：忽略 CR 并读取 LF 前内容")
    void 行协议_忽略Cr并读取Lf前内容() throws Exception {
        assertEquals("AUTH token", AgentProtocol.readLine(input("AUTH token\r\n")));
    }

    private static void assertFrameRoundTrip(String type, String payloadText) throws Exception {
        byte[] payload = payloadText.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        AgentProtocol.writeFrame(new DataOutputStream(bytes), type, payload);

        BufferedInputStream in = input(bytes.toByteArray());
        AgentProtocol.FrameHeader header = AgentProtocol.readFrameHeader(in);

        assertEquals(type, header.getType());
        assertEquals(payload.length, header.getLength());
        assertArrayEquals(payload, AgentProtocol.readFramePayload(in, header.getLength()));
    }

    private static BufferedInputStream input(String text) {
        return input(text.getBytes(StandardCharsets.UTF_8));
    }

    private static BufferedInputStream input(byte[] bytes) {
        return new BufferedInputStream(new ByteArrayInputStream(bytes));
    }
}

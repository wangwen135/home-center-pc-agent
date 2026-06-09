package com.wwh.home.service.handler;

import com.wwh.home.service.CommandExecutionService;
import com.wwh.home.service.dto.CmdResult;
import com.wwh.home.service.protocol.AgentProtocol;
import com.wwh.home.util.ConfigUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientHandlerTest {

    @AfterEach
    void 恢复默认配置() {
        ConfigUtil.reload();
    }

    @Test
    @DisplayName("AUTH 握手：token 正确返回 AUTH OK")
    void auth握手_token正确返回Ok() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Socket socket = socketWithInput("AUTH changeme\n", output);

        new ClientHandler(socket).run();

        assertEquals("AUTH OK\n", output.toString("UTF-8"));
        verify(socket).close();
    }

    @Test
    @DisplayName("AUTH 握手：token 错误返回 AUTH FAILED")
    void auth握手_token错误返回Failed() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Socket socket = socketWithInput("AUTH bad-token\n", output);

        new ClientHandler(socket).run();

        assertEquals("AUTH FAILED\n", output.toString("UTF-8"));
        verify(socket).close();
    }

    @Test
    @DisplayName("普通命令：未知命令返回未知指令")
    void 普通命令_未知命令返回未知指令() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Socket socket = socketWithInput("AUTH changeme\nnot-exists\n", output);

        new ClientHandler(socket).run();

        assertEquals("AUTH OK\n未知指令: not-exists\n", output.toString("UTF-8"));
    }

    @Test
    @DisplayName("CMD 协议：认证后解析 payload 并返回 CMD-RESULT")
    void cmd协议_认证后执行命令并返回结果帧() throws Exception {
        String payload = "{\"command\":\"pwd\",\"timeoutSeconds\":3}";
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        input.write("AUTH changeme\n".getBytes(StandardCharsets.UTF_8));
        AgentProtocol.writeFrame(new DataOutputStream(input), "CMD", payload.getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Socket socket = socketWithInput(input.toByteArray(), output);
        ClientHandler handler = new ClientHandler(socket);

        CommandExecutionService commandService = mock(CommandExecutionService.class);
        CmdResult commandResult = new CmdResult();
        commandResult.setSuccess(true);
        commandResult.setExitCode(0);
        commandResult.setStdout("/tmp");
        when(commandService.execute(eq("pwd"), eq(3), eq(1048576))).thenReturn(commandResult);
        replaceCommandService(handler, commandService);

        handler.run();

        BufferedInputStream resultIn = new BufferedInputStream(new ByteArrayInputStream(output.toByteArray()));
        assertEquals("AUTH OK", AgentProtocol.readLine(resultIn));
        AgentProtocol.FrameHeader header = AgentProtocol.readFrameHeader(resultIn);
        byte[] resultPayload = AgentProtocol.readFramePayload(resultIn, header.getLength());
        String resultJson = new String(resultPayload, StandardCharsets.UTF_8);

        assertEquals("CMD-RESULT", header.getType());
        assertTrue(resultJson.contains("\"success\":true"));
        assertTrue(resultJson.contains("\"stdout\":\"/tmp\""));
        verify(commandService).execute("pwd", 3, 1048576);
    }

    @Test
    @DisplayName("CMD 协议：畸形帧头关闭连接")
    void cmd协议_畸形帧头关闭连接() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Socket socket = socketWithInput("AUTH changeme\nCMD bad\n", output);

        new ClientHandler(socket).run();

        assertEquals("AUTH OK\n", output.toString("UTF-8"));
        verify(socket).close();
    }

    private static Socket socketWithInput(String input, ByteArrayOutputStream output) throws Exception {
        return socketWithInput(input.getBytes(StandardCharsets.UTF_8), output);
    }

    private static Socket socketWithInput(byte[] input, ByteArrayOutputStream output) throws Exception {
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(input));
        when(socket.getOutputStream()).thenReturn(output);
        when(socket.getRemoteSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));
        return socket;
    }

    private static void replaceCommandService(ClientHandler handler, CommandExecutionService service) throws Exception {
        Field field = ClientHandler.class.getDeclaredField("commandExecutionService");
        field.setAccessible(true);
        field.set(handler, service);
    }
}

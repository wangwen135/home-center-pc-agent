package com.wwh.home.service.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CmdRequestTest {

    @Test
    @DisplayName("反序列化：读取 command 和 timeoutSeconds")
    void 反序列化_读取字段完整值() {
        CmdRequest request = CmdRequest.fromJson("{\"command\":\"echo hello\",\"timeoutSeconds\":7}", 60);

        assertEquals("echo hello", request.getCommand());
        assertEquals(7, request.getTimeoutSeconds());
    }

    @Test
    @DisplayName("反序列化：支持字符串转义")
    void 反序列化_支持字符串转义() {
        CmdRequest request = CmdRequest.fromJson("{\"command\":\"printf \\\"a\\\\nb\\\"\"}", 60);

        assertEquals("printf \"a\\nb\"", request.getCommand());
        assertEquals(60, request.getTimeoutSeconds());
    }

    @Test
    @DisplayName("反序列化：缺失字段使用默认值")
    void 反序列化_缺失字段使用默认值() {
        CmdRequest request = CmdRequest.fromJson("{}", 30);

        assertEquals("", request.getCommand());
        assertEquals(30, request.getTimeoutSeconds());
    }

    @Test
    @DisplayName("反序列化：非法 timeout 使用默认值")
    void 反序列化_非法Timeout使用默认值() {
        CmdRequest request = CmdRequest.fromJson("{\"command\":\"pwd\",\"timeoutSeconds\":\"bad\"}", 25);

        assertEquals("pwd", request.getCommand());
        assertEquals(25, request.getTimeoutSeconds());
    }

    @Test
    @DisplayName("非法输入：null JSON 抛出异常")
    void 非法输入_nullJson抛出异常() {
        assertThrows(NullPointerException.class, () -> CmdRequest.fromJson(null, 10));
    }
}

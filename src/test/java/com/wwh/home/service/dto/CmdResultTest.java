package com.wwh.home.service.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CmdResultTest {

    @Test
    @DisplayName("序列化：输出所有字段")
    void 序列化_输出所有字段() {
        CmdResult result = new CmdResult();
        result.setSuccess(true);
        result.setExitCode(0);
        result.setStdout("out");
        result.setStderr("err");
        result.setTimedOut(false);
        result.setTruncated(true);
        result.setDurationMillis(123);
        result.setError("none");

        String json = result.toJson();

        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"exitCode\":0"));
        assertTrue(json.contains("\"stdout\":\"out\""));
        assertTrue(json.contains("\"stderr\":\"err\""));
        assertTrue(json.contains("\"timedOut\":false"));
        assertTrue(json.contains("\"truncated\":true"));
        assertTrue(json.contains("\"durationMillis\":123"));
        assertTrue(json.contains("\"error\":\"none\""));
    }

    @Test
    @DisplayName("序列化：转义特殊字符")
    void 序列化_转义特殊字符() {
        CmdResult result = new CmdResult();
        result.setStdout("a\"b\\c\n");
        result.setStderr("\tstderr");
        result.setError("bad\r\n");

        String json = result.toJson();

        assertTrue(json.contains("\"stdout\":\"a\\\"b\\\\c\\n\""));
        assertTrue(json.contains("\"stderr\":\"\\tstderr\""));
        assertTrue(json.contains("\"error\":\"bad\\r\\n\""));
    }

    @Test
    @DisplayName("序列化：null 字符串输出空串")
    void 序列化_null字符串输出空串() {
        CmdResult result = new CmdResult();

        String json = result.toJson();

        assertTrue(json.contains("\"stdout\":\"\""));
        assertTrue(json.contains("\"stderr\":\"\""));
        assertTrue(json.contains("\"error\":\"\""));
    }
}

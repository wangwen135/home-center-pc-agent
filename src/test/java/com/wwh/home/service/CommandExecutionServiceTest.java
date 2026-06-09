package com.wwh.home.service;

import com.wwh.home.service.dto.CmdResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CommandExecutionServiceTest {

    private final CommandExecutionService service = new CommandExecutionService();

    @Test
    @DisplayName("命令执行：正常命令返回 stdout 和 0 退出码")
    void 命令执行_正常命令返回成功结果() {
        CmdResult result = service.execute("printf 'hello'", 5, 1024);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertEquals("hello", result.getStdout());
        assertEquals("", result.getStderr());
        assertFalse(result.isTimedOut());
        assertFalse(result.isTruncated());
    }

    @Test
    @DisplayName("命令执行：空命令直接返回错误")
    void 命令执行_空命令返回错误() {
        CmdResult result = service.execute("   ", 5, 1024);

        assertFalse(result.isSuccess());
        assertEquals(-1, result.getExitCode());
        assertEquals("command 不能为空", result.getError());
    }

    @Test
    @DisplayName("命令执行：非零退出码标记失败")
    void 命令执行_非零退出码标记失败() {
        CmdResult result = service.execute("sh -c 'echo err >&2; exit 7'", 5, 1024);

        assertFalse(result.isSuccess());
        assertEquals(7, result.getExitCode());
        assertEquals("err\n", result.getStderr());
    }

    @Test
    @DisplayName("输出截断：stdout 超过限制时截断")
    void 输出截断_stdout超过限制时截断() {
        CmdResult result = service.execute("printf 'abcdef'", 5, 3);

        assertEquals("abc", result.getStdout());
        assertTrue(result.isTruncated());
    }

    @Test
    @DisplayName("超时终止：超时命令被标记 timedOut")
    void 超时终止_超时命令被标记TimedOut() {
        CmdResult result = service.execute("sleep 3", 1, 1024);

        assertFalse(result.isSuccess());
        assertTrue(result.isTimedOut());
        assertEquals(-1, result.getExitCode());
        assertTrue(result.getDurationMillis() < 5000);
    }

    @Test
    @DisplayName("进程销毁：waitFor 失败后强制销毁 Mock Process")
    void 进程销毁_waitFor失败后强制销毁MockProcess() throws Exception {
        Process process = mock(Process.class);
        doThrow(new UnsupportedOperationException("无 pid")).when(process).pid();
        doReturn(false).when(process).waitFor(2, TimeUnit.SECONDS);
        doReturn(process).when(process).destroyForcibly();

        Method killProcess = CommandExecutionService.class.getDeclaredMethod("killProcess", Process.class);
        killProcess.setAccessible(true);
        killProcess.invoke(service, process);

        verify(process).destroy();
        verify(process).destroyForcibly();
    }

    @Test
    @DisplayName("异常场景：非法 shell 命令返回错误信息或非零退出")
    void 异常场景_非法Shell命令返回失败() {
        CmdResult result = service.execute("command-that-does-not-exist-wwh", 5, 1024);

        assertFalse(result.isSuccess());
        assertNotNull(result.getStderr());
    }
}

package com.wwh.home.service;

import com.wwh.home.service.dto.CmdResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 远程命令执行服务
 */
public class CommandExecutionService {
    private static final String SETSID_COMMAND = findExecutable("/usr/bin/setsid", "/bin/setsid");

    public CmdResult execute(String command, int timeoutSeconds, int maxOutputBytes) {
        CmdResult result = new CmdResult();
        long start = System.currentTimeMillis();

        if (command == null || command.trim().isEmpty()) {
            result.setSuccess(false);
            result.setExitCode(-1);
            result.setError("command 不能为空");
            result.setDurationMillis(System.currentTimeMillis() - start);
            return result;
        }

        Process process = null;
        OutputLimit outputLimit = new OutputLimit(maxOutputBytes);
        LimitedOutput stdout = new LimitedOutput(outputLimit);
        LimitedOutput stderr = new LimitedOutput(outputLimit);

        try {
            ProcessBuilder builder = createProcessBuilder(command);
            process = builder.start();

            Thread stdoutThread = new Thread(new StreamReader(process.getInputStream(), stdout), "cmd-stdout-reader");
            Thread stderrThread = new Thread(new StreamReader(process.getErrorStream(), stderr), "cmd-stderr-reader");
            stdoutThread.setDaemon(true);
            stderrThread.setDaemon(true);
            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                result.setTimedOut(true);
                killProcess(process);
            }

            stdoutThread.join(2000);
            stderrThread.join(2000);

            int exitCode = result.isTimedOut() ? -1 : process.exitValue();
            result.setExitCode(exitCode);
            result.setSuccess(!result.isTimedOut() && exitCode == 0);
            result.setStdout(stdout.asString());
            result.setStderr(stderr.asString());
            result.setTruncated(stdout.isTruncated() || stderr.isTruncated());
        } catch (Exception e) {
            result.setSuccess(false);
            result.setExitCode(-1);
            result.setError(e.getMessage());
            if (process != null) {
                killProcess(process);
            }
        } finally {
            result.setDurationMillis(System.currentTimeMillis() - start);
        }

        return result;
    }

    private ProcessBuilder createProcessBuilder(String command) {
        if (isWindows()) {
            return new ProcessBuilder("cmd.exe", "/c", command);
        }
        if (SETSID_COMMAND != null) {
            // Linux 下使用独立进程组，超时后可以清理 shell 及其子进程。
            return new ProcessBuilder(SETSID_COMMAND, "/bin/sh", "-c", command);
        }
        return new ProcessBuilder("/bin/sh", "-c", command);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private void killProcess(Process process) {
        Long pid = getProcessId(process);
        try {
            if (isWindows() && pid != null) {
                new ProcessBuilder("taskkill", "/T", "/F", "/PID", String.valueOf(pid)).start().waitFor(5, TimeUnit.SECONDS);
            } else if (pid != null && SETSID_COMMAND != null) {
                killUnixProcessGroup(pid, "-TERM");
            }
        } catch (Exception e) {
            System.err.println("终止进程树失败: " + e.getMessage());
        }

        try {
            process.destroy();
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                if (!isWindows() && pid != null && SETSID_COMMAND != null) {
                    killUnixProcessGroup(pid, "-KILL");
                }
                process.destroyForcibly();
            }
        } catch (Exception e) {
            System.err.println("销毁进程失败: " + e.getMessage());
            process.destroyForcibly();
        }
    }

    private Long getProcessId(Process process) {
        try {
            Object pid = Process.class.getMethod("pid").invoke(process);
            if (pid instanceof Long) {
                return (Long) pid;
            }
        } catch (Exception e) {
            System.err.println("当前 Java 版本不支持获取进程 PID: " + e.getMessage());
        }
        return null;
    }

    private void killUnixProcessGroup(Long pid, String signal) throws IOException, InterruptedException {
        new ProcessBuilder("kill", signal, "-" + pid).start().waitFor(5, TimeUnit.SECONDS);
    }

    private static String findExecutable(String... candidates) {
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.isFile() && file.canExecute()) {
                return candidate;
            }
        }
        return null;
    }

    private static class StreamReader implements Runnable {
        private final InputStream inputStream;
        private final LimitedOutput output;

        private StreamReader(InputStream inputStream, LimitedOutput output) {
            this.inputStream = inputStream;
            this.output = output;
        }

        public void run() {
            byte[] buffer = new byte[4096];
            try {
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    output.write(buffer, read);
                }
            } catch (IOException e) {
                System.err.println("读取命令输出失败: " + e.getMessage());
            }
        }
    }

    private static class LimitedOutput {
        private final OutputLimit outputLimit;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private boolean truncated;

        private LimitedOutput(OutputLimit outputLimit) {
            this.outputLimit = outputLimit;
        }

        private synchronized void write(byte[] bytes, int length) {
            if (length <= 0) {
                return;
            }

            int allowed = outputLimit.reserve(length);
            if (allowed > 0) {
                output.write(bytes, 0, allowed);
            }
            if (allowed < length) {
                truncated = true;
            }
        }

        private synchronized String asString() {
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }

        private synchronized boolean isTruncated() {
            return truncated;
        }
    }

    private static class OutputLimit {
        private final int limit;
        private int written;

        private OutputLimit(int limit) {
            this.limit = Math.max(0, limit);
        }

        private synchronized int reserve(int requested) {
            int remaining = limit - written;
            if (remaining <= 0) {
                return 0;
            }

            int allowed = Math.min(remaining, requested);
            written += allowed;
            return allowed;
        }
    }
}

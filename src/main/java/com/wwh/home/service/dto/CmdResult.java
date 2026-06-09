package com.wwh.home.service.dto;

/**
 * 远程命令执行结果 DTO
 */
public class CmdResult {
    private boolean success;
    private int exitCode;
    private String stdout;
    private String stderr;
    private boolean timedOut;
    private boolean truncated;
    private long durationMillis;
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"success\":").append(success).append(',');
        json.append("\"exitCode\":").append(exitCode).append(',');
        json.append("\"stdout\":\"").append(escape(stdout)).append("\",");
        json.append("\"stderr\":\"").append(escape(stderr)).append("\",");
        json.append("\"timedOut\":").append(timedOut).append(',');
        json.append("\"truncated\":").append(truncated).append(',');
        json.append("\"durationMillis\":").append(durationMillis).append(',');
        json.append("\"error\":\"").append(escape(error)).append("\"");
        json.append('}');
        return json.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        escaped.append("\\u");
                        for (int j = hex.length(); j < 4; j++) {
                            escaped.append('0');
                        }
                        escaped.append(hex);
                    } else {
                        escaped.append(c);
                    }
                    break;
            }
        }
        return escaped.toString();
    }
}

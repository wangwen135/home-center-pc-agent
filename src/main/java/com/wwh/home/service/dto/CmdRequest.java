package com.wwh.home.service.dto;

/**
 * 远程命令请求 DTO
 */
public class CmdRequest {
    private String command;
    private int timeoutSeconds;

    public CmdRequest() {
    }

    public CmdRequest(String command, int timeoutSeconds) {
        this.command = command;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public static CmdRequest fromJson(String json, int defaultTimeoutSeconds) {
        CmdRequest request = new CmdRequest();
        request.setCommand(readStringField(json, "command"));
        request.setTimeoutSeconds(readIntField(json, "timeoutSeconds", defaultTimeoutSeconds));
        return request;
    }

    private static String readStringField(String json, String fieldName) {
        String key = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) {
            return "";
        }

        int colonIndex = json.indexOf(':', keyIndex + key.length());
        if (colonIndex < 0) {
            return "";
        }

        int quoteIndex = findNextNonWhitespace(json, colonIndex + 1);
        if (quoteIndex < 0 || json.charAt(quoteIndex) != '"') {
            return "";
        }

        StringBuilder value = new StringBuilder();
        boolean escaping = false;
        for (int i = quoteIndex + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaping) {
                switch (c) {
                    case '"':
                    case '\\':
                    case '/':
                        value.append(c);
                        break;
                    case 'b':
                        value.append('\b');
                        break;
                    case 'f':
                        value.append('\f');
                        break;
                    case 'n':
                        value.append('\n');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    default:
                        value.append(c);
                        break;
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return value.toString();
    }

    private static int readIntField(String json, String fieldName, int defaultValue) {
        String key = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) {
            return defaultValue;
        }

        int colonIndex = json.indexOf(':', keyIndex + key.length());
        if (colonIndex < 0) {
            return defaultValue;
        }

        int start = findNextNonWhitespace(json, colonIndex + 1);
        if (start < 0) {
            return defaultValue;
        }

        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if ((c >= '0' && c <= '9') || c == '-') {
                end++;
            } else {
                break;
            }
        }

        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static int findNextNonWhitespace(String text, int start) {
        for (int i = start; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}

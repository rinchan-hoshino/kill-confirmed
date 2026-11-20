package dev.rinchan.killconfirmed.portable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal zero-dependency TOML parser and serializer for Kill Confirmed config files. */
public final class SimpleToml {
    private SimpleToml() {}

    public static Map<String, Object> parse(String content) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (content == null || content.isEmpty()) return result;

        int i = 0;
        int len = content.length();
        while (i < len) {
            i = skipWhitespaceAndComments(content, i);
            if (i >= len) break;

            int keyStart = i;
            while (i < len && content.charAt(i) != '=' && content.charAt(i) != '\n' && content.charAt(i) != '\r') {
                i++;
            }
            if (i >= len || content.charAt(i) != '=') {
                i = skipToNextLine(content, i);
                continue;
            }
            String key = content.substring(keyStart, i).trim();
            i++; // consume '='
            i = skipInlineWhitespace(content, i);
            if (i >= len || key.isEmpty()) break;

            char c = content.charAt(i);
            if (c == '[') {
                List<Object> list = new ArrayList<>();
                i++; // consume '['
                while (i < len) {
                    i = skipWhitespaceAndComments(content, i);
                    if (i >= len) break;
                    char cur = content.charAt(i);
                    if (cur == ']') {
                        i++;
                        break;
                    }
                    if (cur == ',') {
                        i++;
                        continue;
                    }
                    if (cur == '"' || cur == '\'') {
                        int[] next = new int[1];
                        list.add(readString(content, i, next));
                        i = next[0];
                    } else {
                        int start = i;
                        i = skipToDelimiter(content, i);
                        String raw = content.substring(start, i).trim();
                        if (!raw.isEmpty()) list.add(parseScalar(raw));
                    }
                }
                result.put(key, list);
            } else if (c == '"' || c == '\'') {
                int[] next = new int[1];
                result.put(key, readString(content, i, next));
                i = next[0];
            } else {
                int valStart = i;
                while (i < len && content.charAt(i) != '#' && content.charAt(i) != '\n' && content.charAt(i) != '\r') {
                    i++;
                }
                String raw = content.substring(valStart, i).trim();
                if (!raw.isEmpty()) {
                    result.put(key, parseScalar(raw));
                }
            }
            i = skipToNextLine(content, i);
        }
        return result;
    }

    private static Object parseScalar(String raw) {
        if ("true".equalsIgnoreCase(raw)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(raw)) return Boolean.FALSE;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    public static String escapeString(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 4);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '"') sb.append('\\').append(c);
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else sb.append(c);
        }
        sb.append('"');
        return sb.toString();
    }

    public static String formatLiteralOrEscaped(String value) {
        if (!value.contains("'") && !value.contains("\n") && !value.contains("\r")) {
            return "'" + value + "'";
        }
        return escapeString(value);
    }

    private static String readString(String s, int start, int[] nextOut) {
        char quote = s.charAt(start);
        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (quote == '"' && c == '\\' && i + 1 < len) {
                char esc = s.charAt(i + 1);
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(esc);
                }
                i += 2;
                continue;
            }
            if (c == quote) {
                i++;
                break;
            }
            if (c == '\n' || c == '\r') break;
            sb.append(c);
            i++;
        }
        nextOut[0] = i;
        return sb.toString();
    }

    private static int skipWhitespaceAndComments(String s, int i) {
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') i++;
            else if (c == '#') i = skipToNextLine(s, i);
            else break;
        }
        return i;
    }

    private static int skipInlineWhitespace(String s, int i) {
        int len = s.length();
        while (i < len && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) i++;
        return i;
    }

    private static int skipToNextLine(String s, int i) {
        int len = s.length();
        while (i < len && s.charAt(i) != '\n') i++;
        return i < len ? i + 1 : len;
    }

    private static int skipToDelimiter(String s, int i) {
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c == ',' || c == ']' || c == '\n' || c == '\r') break;
            i++;
        }
        return i;
    }
}

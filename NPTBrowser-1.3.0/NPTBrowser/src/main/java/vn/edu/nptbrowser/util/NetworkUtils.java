package vn.edu.nptbrowser.util;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** URL normalization that tolerates spaces and Unicode while preserving URL delimiters. */
public final class NetworkUtils {
    private static final Pattern URL_PARTS = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*)://([^/?#]*)(.*)$", Pattern.DOTALL);
    private static final Pattern VALID_PERCENT_ESCAPE = Pattern.compile("[0-9a-fA-F]{2}");

    private NetworkUtils() {
    }

    public static String normalizeHttpUrl(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("URL không được để trống.");
        }

        String candidate = input.trim();
        if (!candidate.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            candidate = "http://" + candidate;
        }

        Matcher parts = URL_PARTS.matcher(candidate);
        if (!parts.matches()) {
            throw new IllegalArgumentException("URL không hợp lệ. Hãy kiểm tra địa chỉ và thử lại.");
        }

        String scheme = parts.group(1).toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("NPT Browser chỉ hỗ trợ URL HTTP/HTTPS.");
        }

        String authority = parts.group(2);
        String tail = parts.group(3);
        if (authority.isBlank()) {
            throw new IllegalArgumentException("URL thiếu tên máy chủ hoặc địa chỉ IP.");
        }

        String userInfo = "";
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            userInfo = authority.substring(0, at + 1);
            authority = authority.substring(at + 1);
        }

        String host;
        String portPart = "";
        if (authority.startsWith("[")) {
            int closing = authority.indexOf(']');
            if (closing < 0) {
                throw new IllegalArgumentException("Địa chỉ IPv6 thiếu dấu ngoặc vuông đóng.");
            }
            host = authority.substring(0, closing + 1);
            String remaining = authority.substring(closing + 1);
            if (!remaining.isEmpty()) {
                if (!remaining.matches(":\\d{1,5}")) {
                    throw new IllegalArgumentException("Cổng trong URL không hợp lệ.");
                }
                portPart = remaining;
            }
        } else {
            int firstColon = authority.indexOf(':');
            int lastColon = authority.lastIndexOf(':');
            if (firstColon >= 0 && firstColon == lastColon) {
                String maybePort = authority.substring(firstColon + 1);
                if (maybePort.matches("\\d{1,5}")) {
                    host = authority.substring(0, firstColon);
                    portPart = ":" + maybePort;
                } else {
                    host = authority;
                }
            } else {
                host = authority;
            }
        }

        if (host.isBlank()) {
            throw new IllegalArgumentException("URL thiếu tên máy chủ hoặc địa chỉ IP.");
        }
        if (!portPart.isEmpty()) {
            try {
                int port = Integer.parseInt(portPart.substring(1));
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException("Cổng phải nằm trong khoảng 1–65535.");
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Cổng trong URL không hợp lệ.", ex);
            }
        }

        String asciiHost;
        try {
            asciiHost = host.startsWith("[") ? host : IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Tên miền trong URL không hợp lệ.", ex);
        }

        String sanitized = scheme + "://" + userInfo + asciiHost + portPart + escapeUnsafeTail(tail);
        try {
            URI uri = new URI(sanitized);
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("URL thiếu tên máy chủ hoặc địa chỉ IP.");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("URL không hợp lệ: " + ex.getReason(), ex);
        }
    }

    /** Percent-encodes whitespace, Unicode and unsafe characters in path/query/fragment. */
    private static String escapeUnsafeTail(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            int charCount = Character.charCount(codePoint);

            if (codePoint == '%' && offset + 2 < value.length()
                    && VALID_PERCENT_ESCAPE.matcher(value.substring(offset + 1, offset + 3)).matches()) {
                out.append(value, offset, offset + 3);
                offset += 3;
                continue;
            }

            boolean unsafe = codePoint <= 0x20 || codePoint >= 0x7f
                    || codePoint == '"' || codePoint == '<' || codePoint == '>'
                    || codePoint == '\\' || codePoint == '^' || codePoint == '`'
                    || codePoint == '{' || codePoint == '}' || codePoint == '[' || codePoint == ']' || codePoint == '|' 
                    || codePoint == '%';
            if (unsafe) {
                byte[] bytes = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    out.append('%');
                    out.append(Character.toUpperCase(Character.forDigit((b >>> 4) & 0xF, 16)));
                    out.append(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
                }
            } else {
                out.appendCodePoint(codePoint);
            }
            offset += charCount;
        }
        return out.toString();
    }
}

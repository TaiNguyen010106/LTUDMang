package vn.edu.nptbrowser.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Converts the omnibox input into an HTTP(S) URL or an UTF-8 web-search URL. */
public final class NavigationInput {
    private static final String SEARCH_ENDPOINT = "https://www.google.com/search?q=";
    private static final String SCHEME_PATTERN = "^[a-zA-Z][a-zA-Z0-9+.-]*://.*$";
    private static final String IPV4_OR_LOCALHOST = "^(localhost|\\d{1,3}(\\.\\d{1,3}){3})(:\\d{1,5})?([/?#].*)?$";
    private static final String DOMAIN = "^(?=.{1,253}(?:$|[/?#]))[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]{0,61}[\\p{L}\\p{N}])?(?:\\.[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]{0,61}[\\p{L}\\p{N}])?)+(?:\\:\\d{1,5})?([/?#].*)?$";

    private NavigationInput() {
    }

    public static String resolve(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Hãy nhập địa chỉ web hoặc từ khóa cần tìm.");
        }

        String candidate = input.trim();
        if (candidate.matches(SCHEME_PATTERN)) {
            return NetworkUtils.normalizeHttpUrl(candidate);
        }

        String lower = candidate.toLowerCase(Locale.ROOT);
        boolean looksLikeAddress = lower.matches(IPV4_OR_LOCALHOST) || candidate.matches(DOMAIN);
        if (looksLikeAddress) {
            // Behave like a modern browser: prefer HTTPS for public domains, but use HTTP
            // for IPs, localhost, intranet suffixes and explicit non-standard ports.
            if (isHttpFirstAddress(lower)) {
                return NetworkUtils.normalizeHttpUrl(candidate);
            }
            return NetworkUtils.normalizeHttpUrl("https://" + candidate);
        }

        // URLEncoder applies UTF-8 and safely escapes +, &, #, %, emoji and Vietnamese accents.
        String encoded = URLEncoder.encode(candidate, StandardCharsets.UTF_8);
        return SEARCH_ENDPOINT + encoded;
    }

    private static boolean isHttpFirstAddress(String lower) {
        String authority = lower.split("[/?#]", 2)[0];
        if (authority.equals("localhost") || authority.startsWith("localhost:")
                || authority.matches("\\d{1,3}(\\.\\d{1,3}){3}(?::\\d{1,5})?")) {
            return true;
        }
        if (authority.matches(".*:\\d{1,5}$")) return true;
        return authority.endsWith(".local") || authority.endsWith(".lan")
                || authority.endsWith(".internal") || authority.endsWith(".test")
                || authority.endsWith(".home.arpa");
    }
}

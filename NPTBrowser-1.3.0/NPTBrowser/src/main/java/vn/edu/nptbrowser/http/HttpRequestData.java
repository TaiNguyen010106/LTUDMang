package vn.edu.nptbrowser.http;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public record HttpRequestData(
        String method,
        URI uri,
        Map<String, List<String>> headers,
        String body,
        Duration timeout) {
}

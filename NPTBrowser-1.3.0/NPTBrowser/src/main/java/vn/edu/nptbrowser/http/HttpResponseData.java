package vn.edu.nptbrowser.http;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public record HttpResponseData(
        String method,
        URI requestedUri,
        URI responseUri,
        int statusCode,
        Map<String, List<String>> headers,
        String body,
        Duration elapsed) {
}

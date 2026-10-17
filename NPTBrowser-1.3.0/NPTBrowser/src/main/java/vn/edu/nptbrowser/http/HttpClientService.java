package vn.edu.nptbrowser.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class HttpClientService {
    private final HttpClient client;

    public HttpClientService(Duration connectTimeout) {
        client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CompletableFuture<HttpResponseData> send(HttpRequestData data) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(data.uri()).timeout(data.timeout());
        data.headers().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));

        String method = data.method().toUpperCase(Locale.ROOT);
        HttpRequest.BodyPublisher publisher = data.body() == null || data.body().isEmpty()
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(data.body(), StandardCharsets.UTF_8);
        builder.method(method, publisher);

        Instant started = Instant.now();
        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> new HttpResponseData(
                        method,
                        data.uri(),
                        response.uri(),
                        response.statusCode(),
                        response.headers().map(),
                        response.body(),
                        Duration.between(started, Instant.now())));
    }

    public static Map<String, List<String>> parseHeaders(String rawHeaders) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        if (rawHeaders == null || rawHeaders.isBlank()) {
            return headers;
        }

        String[] lines = rawHeaders.split("\\R");
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isEmpty()) {
                continue;
            }
            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw new IllegalArgumentException("Header dong " + (index + 1) + " phai co dang Ten: Gia tri");
            }
            String name = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Ten header o dong " + (index + 1) + " bi trong");
            }
            headers.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }
        return headers;
    }

    public static URI parseHttpUri(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null) {
                throw new IllegalArgumentException("URL HTTP Tester phai la HTTP/HTTPS hop le");
            }
            return uri;
        } catch (RuntimeException ex) {
            if (ex instanceof IllegalArgumentException && ex.getMessage() != null
                    && ex.getMessage().startsWith("URL HTTP Tester")) {
                throw ex;
            }
            throw new IllegalArgumentException("URL HTTP Tester khong hop le", ex);
        }
    }
}

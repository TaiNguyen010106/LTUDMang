package vn.edu.nptbrowser.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpClientServiceTest {
    private HttpServer server;
    private URI endpoint;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", exchange -> {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            String response = exchange.getRequestMethod() + ":" + new String(requestBody, StandardCharsets.UTF_8);
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/echo");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsRealPostRequestAsynchronously() {
        HttpClientService service = new HttpClientService(Duration.ofSeconds(2));
        HttpRequestData request = new HttpRequestData(
                "POST", endpoint, Map.of("Content-Type", List.of("text/plain")), "hello", Duration.ofSeconds(2));

        HttpResponseData response = service.send(request).join();

        assertEquals(200, response.statusCode());
        assertEquals("POST:hello", response.body());
        assertTrue(response.elapsed().toMillis() >= 0);
    }

    @Test
    void parsesRepeatedHeaders() {
        Map<String, List<String>> headers = HttpClientService.parseHeaders("Accept: application/json\nX-Test: one\nX-Test: two");
        assertEquals(List.of("one", "two"), headers.get("X-Test"));
    }

    @Test
    void rejectsMalformedHeader() {
        assertThrows(IllegalArgumentException.class, () -> HttpClientService.parseHeaders("Invalid"));
    }
}

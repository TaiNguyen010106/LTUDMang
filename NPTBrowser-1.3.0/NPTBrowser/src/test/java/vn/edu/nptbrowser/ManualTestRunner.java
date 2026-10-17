package vn.edu.nptbrowser;

import com.sun.net.httpserver.HttpServer;
import vn.edu.nptbrowser.config.AppConfig;
import vn.edu.nptbrowser.config.QuickLinkStore;
import vn.edu.nptbrowser.config.QuickLinkStore.QuickLink;
import vn.edu.nptbrowser.http.HttpClientService;
import vn.edu.nptbrowser.http.HttpRequestData;
import vn.edu.nptbrowser.http.HttpResponseData;
import vn.edu.nptbrowser.util.NetworkUtils;
import vn.edu.nptbrowser.util.NavigationInput;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class ManualTestRunner {
    private int passed;
    private int failed;

    public static void main(String[] args) throws Exception {
        ManualTestRunner runner = new ManualTestRunner();
        runner.run();
    }

    private void run() throws Exception {
        check("Them http:// khi URL thieu scheme",
                "http://192.168.1.2/".equals(NetworkUtils.normalizeHttpUrl("192.168.1.2/")));
        check("Giu nguyen HTTPS hop le",
                "https://example.com/a?q=1".equals(NetworkUtils.normalizeHttpUrl("https://example.com/a?q=1")));
        checkThrows("Tu choi file://", () -> NetworkUtils.normalizeHttpUrl("file:///tmp/a"));
        checkThrows("Tu choi URL thieu host", () -> NetworkUtils.normalizeHttpUrl("http:///abc"));
        check("Thanh dia chi nhan IP va port", "http://localhost:5001/api/time".equals(
                NavigationInput.resolve("localhost:5001/api/time")));
        check("Ten mien co HTTPS mo truc tiep", "https://example.com".equals(
                NavigationInput.resolve("https://example.com")));
        check("Ten mien cong khai tu dong uu tien HTTPS", "https://example.com".equals(
                NavigationInput.resolve("example.com")));
        check("Tu khoa duoc chuyen thanh tim kiem", "https://www.google.com/search?q=java+desktop".equals(
                NavigationInput.resolve("java desktop")));
        check("Tu khoa tieng Viet va ky tu dac biet duoc ma hoa UTF-8", (
                "https://www.google.com/search?q=C%2B%2B+%26+Java+%231+ti%E1%BA%BFng+Vi%E1%BB%87t+%25")
                .equals(NavigationInput.resolve("C++ & Java #1 tiếng Việt %")));
        check("URL co khoang trang va tieng Viet duoc ma hoa", (
                "https://example.com/search?q=ti%E1%BA%BFng%20Vi%E1%BB%87t&n=100%25")
                .equals(NetworkUtils.normalizeHttpUrl("https://example.com/search?q=tiếng Việt&n=100%")));
        check("URL khong scheme co path va query Unicode duoc mo truc tiep", (
                "https://example.com/search?q=ti%E1%BA%BFng%20Vi%E1%BB%87t&x=100%25")
                .equals(NavigationInput.resolve("example.com/search?q=tiếng Việt&x=100%")));
        check("Dau ngoac va khoang trang trong URL duoc escape", (
                "https://example.com/search?q=%5BC++%5D%20%7BJava%7D")
                .equals(NetworkUtils.normalizeHttpUrl("https://example.com/search?q=[C++] {Java}")));
        check("URL ten mien Unicode duoc chuyen thanh IDN", (
                "https://xn--bcher-kva.example/")
                .equals(NetworkUtils.normalizeHttpUrl("https://bücher.example/")));
        check("Tu don duoc xu ly nhu tim kiem", NavigationInput.resolve("java").startsWith(
                "https://www.google.com/search?q="));

        Map<String, List<String>> headers = HttpClientService.parseHeaders(
                "Accept: application/json\nX-Test: one\nX-Test: two");
        check("Parse header lap", List.of("one", "two").equals(headers.get("X-Test")));
        checkThrows("Tu choi header sai dinh dang", () -> HttpClientService.parseHeaders("Invalid"));

        Path tempDir = Files.createTempDirectory("npt-config-test-");
        Path configFile = tempDir.resolve("application.properties");
        AppConfig config = AppConfig.load(configFile);
        config.updateUserSettings("http://10.0.0.10/", "http://10.0.0.10:5001", 15);
        AppConfig reloaded = AppConfig.load(configFile);
        check("Luu va doc cau hinh", reloaded.homeUrl().equals("http://10.0.0.10/")
                && reloaded.apiBaseUrl().equals("http://10.0.0.10:5001")
                && reloaded.timeoutSeconds() == 15);

        Path quickLinksFile = tempDir.resolve("quick-links.properties");
        QuickLinkStore links = QuickLinkStore.load(quickLinksFile, "http://192.168.1.2/");
        check("Kho loi tat tao bon trang mac dinh", links.list().size() == 4);
        QuickLink special = new QuickLink("Tài liệu C++ & Java", "https://example.com/?q=C%2B%2B%26Java", "◈", "68AAFF");
        links.add(special);
        QuickLinkStore savedLinks = QuickLinkStore.load(quickLinksFile, "http://192.168.1.2/");
        check("Luu va doc ten tieng Viet va ky tu dac biet trong loi tat",
                savedLinks.list().stream().anyMatch(link -> link.title().equals("Tài liệu C++ & Java")
                        && link.url().equals("https://example.com/?q=C%2B%2B%26Java")));
        checkThrows("Tu choi loi tat URL bi trung", () -> savedLinks.add(special));
        for (QuickLink link : savedLinks.list()) savedLinks.remove(link);
        check("Cho phep xoa het loi tat va giu trang thai sau khoi dong lai",
                QuickLinkStore.load(quickLinksFile, "http://192.168.1.2/").list().isEmpty());

        testRealAsyncPost();

        System.out.printf("MANUAL TESTS: %d dat, %d khong dat%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private void testRealAsyncPost() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", exchange -> {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            String response = exchange.getRequestMethod() + ":"
                    + new String(requestBody, StandardCharsets.UTF_8);
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/echo");
            HttpClientService service = new HttpClientService(Duration.ofSeconds(2));
            HttpRequestData request = new HttpRequestData(
                    "POST", endpoint, Map.of("Content-Type", List.of("text/plain")),
                    "hello", Duration.ofSeconds(2));
            HttpResponseData response = service.send(request).join();
            check("Gui POST bat dong bo toi HTTP server that",
                    response.statusCode() == 200 && response.body().equals("POST:hello"));
        } finally {
            server.stop(0);
        }
    }

    private void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[DAT] " + name);
        } else {
            failed++;
            System.out.println("[KHONG DAT] " + name);
        }
    }

    private void checkThrows(String name, ThrowingAction action) {
        try {
            action.run();
            check(name, false);
        } catch (IllegalArgumentException expected) {
            check(name, true);
        } catch (Exception unexpected) {
            check(name, false);
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}

package vn.edu.nptbrowser.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public final class AppConfig {
    public static final String CONFIG_PROPERTY = "npt.config";

    private final Properties values = new Properties();
    private final Path configPath;

    private AppConfig(Path configPath) {
        this.configPath = configPath;
    }

    public static AppConfig load() throws IOException {
        String configuredPath = System.getProperty(CONFIG_PROPERTY, "config/application.properties");
        return load(Paths.get(configuredPath));
    }

    public static AppConfig load(Path path) throws IOException {
        AppConfig config = new AppConfig(path.toAbsolutePath().normalize());
        try (InputStream defaults = AppConfig.class.getResourceAsStream("/default-application.properties")) {
            if (defaults == null) {
                throw new IOException("Khong tim thay default-application.properties");
            }
            config.values.load(defaults);
        }
        if (Files.exists(config.configPath)) {
            try (InputStream input = Files.newInputStream(config.configPath)) {
                config.values.load(input);
            }
        }
        return config;
    }

    public String homeUrl() {
        return resolveServerHost(required("home.url"));
    }

    public String apiBaseUrl() {
        return resolveServerHost(required("api.baseUrl")).replaceAll("/+$", "");
    }

    public String serverHost() {
        return required("server.host");
    }

    public int timeoutSeconds() {
        String raw = required("http.timeoutSeconds");
        try {
            int timeout = Integer.parseInt(raw);
            if (timeout < 1 || timeout > 300) {
                throw new IllegalArgumentException("http.timeoutSeconds phai trong khoang 1-300");
            }
            return timeout;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("http.timeoutSeconds khong phai so hop le: " + raw, ex);
        }
    }

    public Path jcefInstallDir() {
        return Paths.get(required("jcef.installDir")).toAbsolutePath().normalize();
    }

    public Path browserCacheDir() {
        return Paths.get(required("browser.cacheDir")).toAbsolutePath().normalize();
    }

    public String acceptLanguage() {
        return required("browser.acceptLanguage");
    }

    public Path configPath() {
        return configPath;
    }

    public void updateUserSettings(String homeUrl, String apiBaseUrl, int timeoutSeconds) throws IOException {
        values.setProperty("home.url", Objects.requireNonNull(homeUrl).trim());
        values.setProperty("api.baseUrl", Objects.requireNonNull(apiBaseUrl).trim());
        values.setProperty("http.timeoutSeconds", Integer.toString(timeoutSeconds));
        timeoutSeconds();

        Path parent = configPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream output = Files.newOutputStream(configPath)) {
            values.store(output, "NPT Browser - cau hinh co the thay doi");
        }
    }

    private String required(String key) {
        String value = values.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Thieu cau hinh: " + key);
        }
        return value.trim();
    }

    private String resolveServerHost(String value) {
        return value.replace("${server.host}", serverHost());
    }
}

package vn.edu.nptbrowser.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsEditableSettings() throws Exception {
        Path file = tempDir.resolve("application.properties");
        AppConfig config = AppConfig.load(file);
        config.updateUserSettings("http://10.0.0.10/", "http://10.0.0.10:5001", 15);

        AppConfig reloaded = AppConfig.load(file);
        assertEquals("http://10.0.0.10/", reloaded.homeUrl());
        assertEquals("http://10.0.0.10:5001", reloaded.apiBaseUrl());
        assertEquals(15, reloaded.timeoutSeconds());
        assertTrue(file.toFile().isFile());
    }

    @Test
    void resolvesServerHostPlaceholders() throws Exception {
        Path file = tempDir.resolve("application.properties");
        java.nio.file.Files.writeString(file, """
                server.host=VM-SERVER
                home.url=http://${server.host}/
                api.baseUrl=http://${server.host}:5001
                """);

        AppConfig config = AppConfig.load(file);

        assertEquals("VM-SERVER", config.serverHost());
        assertEquals("http://VM-SERVER/", config.homeUrl());
        assertEquals("http://VM-SERVER:5001", config.apiBaseUrl());
    }
}

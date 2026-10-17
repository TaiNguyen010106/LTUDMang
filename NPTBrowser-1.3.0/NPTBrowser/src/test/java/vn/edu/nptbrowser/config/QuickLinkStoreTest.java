package vn.edu.nptbrowser.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QuickLinkStoreTest {
    @TempDir Path tempDir;

    @Test
    void seedsDefaultsWhenNoStoreExists() throws Exception {
        QuickLinkStore store = QuickLinkStore.load(tempDir.resolve("quick-links.properties"), "http://192.168.1.2/");
        assertEquals(4, store.list().size());
        assertEquals("Shrimple", store.list().get(0).title());
    }

    @Test
    void persistsUnicodeAndSpecialCharactersAcrossReload() throws Exception {
        Path path = tempDir.resolve("quick-links.properties");
        QuickLinkStore store = QuickLinkStore.load(path, "http://192.168.1.2/");
        QuickLinkStore.QuickLink link = new QuickLinkStore.QuickLink(
                "Tài liệu C++ & Java #1", "https://example.com/?q=C%2B%2B%26Java", "◈", "68AAFF");
        store.add(link);
        QuickLinkStore reloaded = QuickLinkStore.load(path, "http://192.168.1.2/");
        assertTrue(reloaded.list().contains(link));
    }

    @Test
    void rejectsDuplicateUrl() throws Exception {
        QuickLinkStore store = QuickLinkStore.load(tempDir.resolve("quick-links.properties"), "http://192.168.1.2/");
        QuickLinkStore.QuickLink link = store.list().get(0);
        assertThrows(IllegalArgumentException.class, () -> store.add(link));
    }

    @Test
    void canPersistEmptyListAfterDeletingAllLinks() throws Exception {
        Path path = tempDir.resolve("quick-links.properties");
        QuickLinkStore store = QuickLinkStore.load(path, "http://192.168.1.2/");
        for (QuickLinkStore.QuickLink link : store.list()) store.remove(link);
        assertTrue(QuickLinkStore.load(path, "http://192.168.1.2/").list().isEmpty());
    }
}

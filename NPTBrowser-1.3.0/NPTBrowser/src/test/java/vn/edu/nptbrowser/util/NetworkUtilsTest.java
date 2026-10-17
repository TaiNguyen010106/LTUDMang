package vn.edu.nptbrowser.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkUtilsTest {
    @Test
    void addsHttpSchemeWhenMissing() {
        assertEquals("http://192.168.1.2/", NetworkUtils.normalizeHttpUrl("192.168.1.2/"));
    }

    @Test
    void preservesHttpsUrl() {
        assertEquals("https://example.com/a?q=1", NetworkUtils.normalizeHttpUrl("https://example.com/a?q=1"));
    }


    @Test
    void safelyEncodesSpacesUnicodeAndBarePercentInQuery() {
        assertEquals("https://example.com/search?q=ti%E1%BA%BFng%20Vi%E1%BB%87t&n=100%25",
                NetworkUtils.normalizeHttpUrl("https://example.com/search?q=tiếng Việt&n=100%"));
    }

    @Test
    void escapesSpecialDelimitersAndSpacesInsideQuery() {
        assertEquals("https://example.com/search?q=%5BC++%5D%20%7BJava%7D",
                NetworkUtils.normalizeHttpUrl("https://example.com/search?q=[C++] {Java}"));
    }

    @Test
    void convertsInternationalHostToIdn() {
        assertEquals("https://xn--bcher-kva.example/",
                NetworkUtils.normalizeHttpUrl("https://bücher.example/"));
    }

    @Test
    void rejectsUnsupportedScheme() {
        assertThrows(IllegalArgumentException.class, () -> NetworkUtils.normalizeHttpUrl("file:///etc/passwd"));
    }

    @Test
    void rejectsMissingHost() {
        assertThrows(IllegalArgumentException.class, () -> NetworkUtils.normalizeHttpUrl("http:///abc"));
    }
}

package vn.edu.nptbrowser.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationInputTest {
    @Test
    void opensIpAddressDirectly() {
        assertEquals("http://192.168.1.2/", NavigationInput.resolve("192.168.1.2/"));
    }

    @Test
    void opensLocalhostWithPortDirectly() {
        assertEquals("http://localhost:5001/api/time", NavigationInput.resolve("localhost:5001/api/time"));
    }

    @Test
    void opensBareDomainDirectly() {
        assertEquals("https://example.com", NavigationInput.resolve("example.com"));
    }

    @Test
    void searchesForMultipleWords() {
        assertEquals("https://www.google.com/search?q=java+desktop", NavigationInput.resolve("java desktop"));
    }

    @Test
    void searchesForSingleWord() {
        assertTrue(NavigationInput.resolve("java").startsWith("https://www.google.com/search?q="));
    }


    @Test
    void encodesVietnameseAndSpecialCharactersAsUtf8() {
        assertEquals("https://www.google.com/search?q=C%2B%2B+%26+Java+%231+ti%E1%BA%BFng+Vi%E1%BB%87t+%25",
                NavigationInput.resolve("C++ & Java #1 tiếng Việt %"));
    }

    @Test
    void opensUrlWithUnicodeAndSpacesInQuery() {
        assertEquals("https://example.com/search?q=ti%E1%BA%BFng%20Vi%E1%BB%87t&x=100%25",
                NavigationInput.resolve("example.com/search?q=tiếng Việt&x=100%"));
    }

    @Test
    void convertsInternationalDomainToAscii() {
        assertEquals("https://xn--bcher-kva.example/",
                NavigationInput.resolve("https://bücher.example/"));
    }

    @Test
    void rejectsUnsupportedExplicitScheme() {
        assertThrows(IllegalArgumentException.class, () -> NavigationInput.resolve("file:///tmp/test"));
    }
}

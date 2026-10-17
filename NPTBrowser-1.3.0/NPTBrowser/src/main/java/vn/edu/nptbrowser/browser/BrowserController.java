package vn.edu.nptbrowser.browser;

import org.cef.browser.CefBrowser;
import vn.edu.nptbrowser.util.NetworkUtils;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Objects;
import java.util.function.Supplier;

public final class BrowserController {
    private final CefBrowser browser;
    private final Component parent;
    private final Supplier<String> homeUrl;

    public BrowserController(CefBrowser browser, Component parent, Supplier<String> homeUrl) {
        this.browser = Objects.requireNonNull(browser);
        this.parent = Objects.requireNonNull(parent);
        this.homeUrl = Objects.requireNonNull(homeUrl);
    }

    public void navigate(String input) {
        try {
            browser.loadURL(NetworkUtils.normalizeHttpUrl(input));
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "URL khong hop le", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void back() {
        if (browser.canGoBack()) {
            browser.goBack();
        }
    }

    public void forward() {
        if (browser.canGoForward()) {
            browser.goForward();
        }
    }

    public void reload() {
        browser.reload();
    }

    public void stop() {
        browser.stopLoad();
    }

    public void home() {
        navigate(homeUrl.get());
    }
}

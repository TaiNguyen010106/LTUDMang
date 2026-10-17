package vn.edu.nptbrowser.browser;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefFocusHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import vn.edu.nptbrowser.config.AppConfig;
import vn.edu.nptbrowser.config.QuickLinkStore;
import vn.edu.nptbrowser.config.QuickLinkStore.QuickLink;
import vn.edu.nptbrowser.util.NavigationInput;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** SPACE-themed multi-tab Chromium browser with a custom home screen and hover tab dock. */
public final class BrowserPanel extends JPanel {
    private static final Color INK = new Color(5, 10, 25);
    private static final Color PANEL = new Color(7, 14, 34);
    private static final Color ACCENT = new Color(111, 72, 255);
    private static final Color CYAN = new Color(39, 214, 255);
    private static final Color TEXT = new Color(245, 248, 255);
    private static final Color MUTED = new Color(169, 187, 220);
    private static final Color ERROR = new Color(255, 119, 145);
    private static final int RAIL_WIDTH = 66;
    private final CefApp cefApp;
    private final AppConfig config;
    private final Runnable settingsAction;
    private final QuickLinkStore quickLinkStore;
    private final List<HomePage> homePages = new ArrayList<>();
    private final List<BrowserTab> tabs = new ArrayList<>();
    private final Map<String, String> bookmarks = new LinkedHashMap<>();
    private final AtomicInteger tabSequence = new AtomicInteger();
    private final JPanel workspace = new JPanel(new CardLayout());
    private final JPanel railTabList = new JPanel();
    private final JPanel expandedTabList = new JPanel();
    private final JPanel expandedTabs = new JPanel(new BorderLayout(0, 9)) {
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(13, 23, 59, 250), getWidth(), getHeight(), new Color(7, 13, 34, 250)));
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 22, 22));
            g.setColor(new Color(87, 111, 218, 150));
            g.draw(new RoundRectangle2D.Float(.5f, .5f, getWidth() - 2, getHeight() - 2, 22, 22));
            g.dispose();
            super.paintComponent(graphics);
        }
    };
    private final JPanel rail = new JPanel(new BorderLayout());
    private final JLayeredPane layers;
    private final JTextField addressField = new PlaceholderField("Nhập URL hoặc tìm kiếm trên web...");
    private final JLabel statusLabel = new JLabel("Sẵn sàng cho hành trình mới");
    private final JLabel engineStatus = new JLabel("● Đã kết nối");
    private final JProgressBar progressBar = new JProgressBar();
    private final ActionButton backButton = new ActionButton("‹", "Quay lại", false);
    private final ActionButton forwardButton = new ActionButton("›", "Tiến tới", false);
    private final ActionButton reloadButton = new ActionButton("↻", "Tải lại", false);
    private final ActionButton bookmarkButton = new ActionButton("☆", "Lưu dấu trang", false);
    private final ActionButton settingsButton = new ActionButton("⚙", "Cài đặt NPT Browser", false);
    private final ActionButton addTabButton = new ActionButton("+", "Mở tab mới (Ctrl+T)", true);
    private BrowserTab activeTab;
    private boolean expandedByPointer;

    public BrowserPanel(CefApp cefApp, AppConfig config) {
        this(cefApp, config, () -> { });
    }

    public BrowserPanel(CefApp cefApp, AppConfig config, Runnable settingsAction) {
        super(new BorderLayout(0, 0));
        this.cefApp = cefApp;
        this.config = config;
        this.settingsAction = settingsAction == null ? () -> { } : settingsAction;
        QuickLinkStore loadedLinks;
        try {
            loadedLinks = QuickLinkStore.load(config.configPath().resolveSibling("quick-links.properties"), config.homeUrl());
        } catch (IOException ex) {
            loadedLinks = null;
        }
        this.quickLinkStore = loadedLinks;
        setBackground(INK);
        layers = new JLayeredPane() {
            @Override public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                workspace.setBounds(RAIL_WIDTH, 0, Math.max(0, w - RAIL_WIDTH), h);
                rail.setBounds(0, 0, RAIL_WIDTH, h);
                int expandedHeight = Math.min(Math.max(250, expandedTabs.getPreferredSize().height), Math.max(200, h - 24));
                expandedTabs.setBounds(RAIL_WIDTH + 12, 14, 250, expandedHeight);
            }
        };
        buildUi();
        installActions();
        addBrowserTab("", true, false, true);
        if (quickLinkStore == null) {
            statusLabel.setForeground(ERROR);
            statusLabel.setText("Không đọc được quick-links.properties; hãy kiểm tra quyền ghi thư mục config.");
        }
    }

    private void buildUi() {
        JPanel toolbar = new JPanel(new BorderLayout(13, 0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, new Color(6, 12, 31), getWidth(), getHeight(), new Color(14, 23, 58)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(69, 97, 195, 95));
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(9, 13, 10, 14));

        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        navButtons.setOpaque(false);
        for (ActionButton button : List.of(backButton, forwardButton, reloadButton)) {
            button.setPreferredSize(new Dimension(38, 38));
            navButtons.add(button);
        }
        toolbar.add(navButtons, BorderLayout.WEST);

        JPanel addressShell = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, new Color(4, 10, 28, 245), getWidth(), getHeight(), new Color(8, 17, 43, 245)));
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 30, 30));
                g.setPaint(new GradientPaint(0, 0, new Color(47, 206, 255), getWidth(), getHeight(), new Color(153, 90, 255)));
                g.draw(new RoundRectangle2D.Float(.5f, .5f, getWidth() - 2, getHeight() - 2, 30, 30));
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        addressShell.setOpaque(false);
        addressShell.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 7));
        JLabel searchIcon = new JLabel("⌕");
        searchIcon.setForeground(new Color(205, 224, 255));
        searchIcon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 29));
        addressShell.add(searchIcon, BorderLayout.WEST);
        addressField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        addressField.setHorizontalAlignment(SwingConstants.LEFT);
        addressField.setForeground(TEXT);
        addressField.setCaretColor(CYAN);
        addressField.setSelectionColor(new Color(83, 63, 174));
        addressField.setSelectedTextColor(Color.WHITE);
        addressField.setOpaque(false);
        addressField.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        addressField.setToolTipText("Nhập địa chỉ web hoặc từ khóa tìm kiếm");
        addressShell.add(addressField, BorderLayout.CENTER);
        addressShell.setPreferredSize(new Dimension(500, 44));
        toolbar.add(addressShell, BorderLayout.CENTER);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        rightActions.setOpaque(false);
        bookmarkButton.setPreferredSize(new Dimension(40, 40));
        settingsButton.setPreferredSize(new Dimension(40, 40));
        rightActions.add(bookmarkButton);
        rightActions.add(settingsButton);
        toolbar.add(rightActions, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        workspace.setBackground(INK);
        workspace.setOpaque(true);
        layers.setOpaque(true);
        layers.setBackground(INK);
        layers.add(workspace, Integer.valueOf(JLayeredPane.DEFAULT_LAYER));
        buildRail();
        buildExpandedTabs();
        layers.add(rail, Integer.valueOf(JLayeredPane.PALETTE_LAYER));
        layers.add(expandedTabs, Integer.valueOf(JLayeredPane.MODAL_LAYER));
        expandedTabs.setVisible(false);
        add(layers, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, new Color(5, 12, 31), getWidth(), 0, new Color(10, 17, 43)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(76, 94, 182, 130));
                g.drawLine(0, 0, getWidth(), 0);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        statusBar.setOpaque(false);
        statusBar.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 17));
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusBar.add(statusLabel, BorderLayout.CENTER);
        JPanel statusRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        statusRight.setOpaque(false);
        JLabel privacy = new JLabel("⬡  Chế độ riêng tư: Tắt");
        privacy.setForeground(new Color(188, 204, 236));
        privacy.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        engineStatus.setForeground(new Color(99, 255, 202));
        engineStatus.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusRight.add(privacy);
        statusRight.add(engineStatus);
        progressBar.setPreferredSize(new Dimension(92, 4));
        progressBar.setBorderPainted(false);
        progressBar.setForeground(CYAN);
        progressBar.setBackground(new Color(39, 49, 80));
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        statusRight.add(progressBar);
        statusBar.add(statusRight, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        setBorder(BorderFactory.createLineBorder(new Color(39, 66, 137, 180), 1));
    }

    private void buildRail() {
        rail.setOpaque(false);
        rail.setBackground(PANEL);
        rail.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(56, 84, 166, 170)));
        rail.setLayout(new BorderLayout(0, 8));
        JPanel topIcons = new JPanel();
        topIcons.setOpaque(false);
        topIcons.setBorder(BorderFactory.createEmptyBorder(12, 8, 7, 8));
        topIcons.setLayout(new BoxLayout(topIcons, BoxLayout.Y_AXIS));
        ActionButton logo = new ActionButton("N", "NPT Browser", true);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setPreferredSize(new Dimension(46, 46));
        logo.setMaximumSize(new Dimension(46, 46));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.addActionListener(e -> goHome());
        ActionButton home = new ActionButton("⌂", "Trang chủ", false);
        home.setPreferredSize(new Dimension(46, 46));
        home.setMaximumSize(new Dimension(46, 46));
        home.setAlignmentX(Component.CENTER_ALIGNMENT);
        home.addActionListener(e -> goHome());
        topIcons.add(logo);
        topIcons.add(Box.createVerticalStrut(15));
        topIcons.add(home);
        rail.add(topIcons, BorderLayout.NORTH);

        railTabList.setOpaque(false);
        railTabList.setLayout(new BoxLayout(railTabList, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(railTabList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(3, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(26);
        rail.add(scroll, BorderLayout.CENTER);

        JPanel lower = new JPanel();
        lower.setOpaque(false);
        lower.setBorder(BorderFactory.createEmptyBorder(5, 8, 12, 8));
        lower.setLayout(new BoxLayout(lower, BoxLayout.Y_AXIS));
        addTabButton.setPreferredSize(new Dimension(46, 46));
        addTabButton.setMaximumSize(new Dimension(46, 46));
        addTabButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addTabButton.addActionListener(e -> newTab());
        ActionButton testerHint = new ActionButton("⚡", "Chuyển sang HTTP Tester ở thanh trên", false);
        testerHint.setPreferredSize(new Dimension(46, 42));
        testerHint.setMaximumSize(new Dimension(46, 42));
        testerHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        testerHint.addActionListener(e -> statusLabel.setText("Mở chế độ HTTP Tester ở thanh tiêu đề phía trên."));
        lower.add(addTabButton);
        lower.add(Box.createVerticalStrut(9));
        lower.add(testerHint);
        rail.add(lower, BorderLayout.SOUTH);

        installHoverTracking(rail);
    }

    private void buildExpandedTabs() {
        expandedTabs.setOpaque(false);
        expandedTabs.setBackground(new Color(8, 15, 39, 246));
        expandedTabs.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.LineBorder(new Color(95, 102, 246, 210), 1, true),
                BorderFactory.createEmptyBorder(11, 11, 11, 11)));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel headingText = new JLabel("CÁC TAB ĐANG MỞ");
        headingText.setForeground(new Color(163, 190, 255));
        headingText.setFont(new Font("Segoe UI", Font.BOLD, 10));
        heading.add(headingText, BorderLayout.CENTER);
        expandedTabs.add(heading, BorderLayout.NORTH);

        expandedTabList.setOpaque(false);
        expandedTabList.setLayout(new BoxLayout(expandedTabList, BoxLayout.Y_AXIS));
        JScrollPane listScroll = new JScrollPane(expandedTabList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);
        listScroll.getVerticalScrollBar().setPreferredSize(new Dimension(3, 0));
        expandedTabs.add(listScroll, BorderLayout.CENTER);
        ActionButton add = new ActionButton("＋   Tab mới", "Mở tab mới", true);
        add.setPreferredSize(new Dimension(220, 37));
        add.addActionListener(e -> newTab());
        expandedTabs.add(add, BorderLayout.SOUTH);
        installHoverTracking(expandedTabs);
    }

    private void installActions() {
        backButton.addActionListener(e -> withActive(tab -> { if (!tab.isHome && tab.browser.canGoBack()) tab.browser.goBack(); }));
        forwardButton.addActionListener(e -> withActive(tab -> { if (!tab.isHome && tab.browser.canGoForward()) tab.browser.goForward(); }));
        reloadButton.addActionListener(e -> withActive(tab -> {
            if (tab.isHome) return;
            if (tab.loading) tab.browser.stopLoad(); else tab.browser.reload();
        }));
        addressField.addActionListener(e -> navigateAddress(addressField.getText()));
        addressField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (addressField.isFocusOwner()) addressField.selectAll();
                });
            }
        });
        addressField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && activeTab != null) {
                    addressField.setText(activeTab.isHome ? "" : activeTab.currentUrl);
                    activeTab.browser.setFocus(true);
                    e.consume();
                }
            }
        });
        bookmarkButton.addActionListener(e -> toggleBookmark());
        settingsButton.addActionListener(e -> settingsAction.run());
    }

    private void addBrowserTab(String initialUrl, boolean activate, boolean focusAddress, boolean homeScreen) {
        BrowserTab tab = new BrowserTab(tabSequence.incrementAndGet());
        tab.isHome = homeScreen;
        tab.currentUrl = homeScreen ? "npt://home" : initialUrl;
        tab.title = homeScreen ? "Trang chủ" : "Tab mới " + tab.number;
        tab.client = cefApp.createClient();
        tab.browser = tab.client.createBrowser("about:blank", false, false);
        tab.browserComponent = tab.browser.getUIComponent();
        tab.browserComponent.setBackground(Color.BLACK);
        tab.localCards = new JPanel(new CardLayout());
        tab.homePage = new HomePage(quickLinkStore, url -> navigateInTab(tab, url, true),
                this::addQuickLinkFromDialog, this::removeQuickLink);
        homePages.add(tab.homePage);
        tab.localCards.add(tab.homePage, "home");
        tab.browserComponent.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { }
        });
        tab.localCards.add(tab.browserComponent, "browser");
        tab.localLayout = (CardLayout) tab.localCards.getLayout();
        tab.localLayout.show(tab.localCards, homeScreen ? "home" : "browser");
        workspace.add(tab.localCards, tab.cardKey);
        tabs.add(tab);
        installBrowserHandlers(tab);
        rebuildRailTabs();
        rebuildExpandedTabs();
        if (!homeScreen && initialUrl != null && !initialUrl.isBlank()) {
            tab.isHome = false;
            tab.localLayout.show(tab.localCards, "browser");
            tab.browser.loadURL(initialUrl);
        }
        if (activate) selectTab(tab, focusAddress);
        workspace.revalidate();
        workspace.repaint();
    }

    private void installBrowserHandlers(BrowserTab tab) {
        tab.client.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override public void onAddressChange(CefBrowser source, CefFrame frame, String url) {
                if (!frame.isMain() || url == null || url.isBlank() || "about:blank".equals(url)) return;
                SwingUtilities.invokeLater(() -> {
                    tab.currentUrl = url;
                    if (activeTab == tab && !addressField.hasFocus()) addressField.setText(url);
                    if (!url.startsWith("npt:")) {
                        tab.isHome = false;
                        tab.localLayout.show(tab.localCards, "browser");
                    }
                    refreshTabSurfaces();
                });
            }

            @Override public void onTitleChange(CefBrowser source, String title) {
                SwingUtilities.invokeLater(() -> {
                    if (title != null && !title.isBlank() && !tab.isHome) tab.title = title.trim();
                    refreshTabSurfaces();
                });
            }

            @Override public void onStatusMessage(CefBrowser source, String value) {
                if (value != null && !value.isBlank()) SwingUtilities.invokeLater(() -> {
                    tab.status = value;
                    if (activeTab == tab) {
                        statusLabel.setForeground(MUTED);
                        statusLabel.setText(value);
                    }
                });
            }
        });
        tab.client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override public void onLoadingStateChange(CefBrowser source, boolean loading, boolean canGoBack, boolean canGoForward) {
                SwingUtilities.invokeLater(() -> {
                    tab.loading = loading;
                    tab.canGoBack = canGoBack;
                    tab.canGoForward = canGoForward;
                    if (activeTab == tab) {
                        progressBar.setVisible(loading);
                        reloadButton.setText(loading ? "×" : "↻");
                        reloadButton.setToolTipText(loading ? "Dừng tải trang" : "Tải lại trang");
                        statusLabel.setForeground(MUTED);
                        if (loading) statusLabel.setText("Đang tải nội dung...");
                        else if (!tab.isHome) statusLabel.setText("Đã tải xong · " + shortHost(tab.currentUrl));
                        refreshNavigationButtons();
                    }
                    refreshTabSurfaces();
                });
            }

            @Override public void onLoadError(CefBrowser source, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
                if (frame.isMain() && errorCode != CefLoadHandler.ErrorCode.ERR_ABORTED) SwingUtilities.invokeLater(() -> {
                    // If Google search is unreachable on this network, retry the same UTF-8 query with Bing once.
                    if (!tab.searchFallbackAttempted && failedUrl != null
                            && failedUrl.startsWith("https://www.google.com/search?")) {
                        tab.searchFallbackAttempted = true;
                        int queryStart = failedUrl.indexOf('?');
                        String fallbackUrl = "https://www.bing.com/search?" + failedUrl.substring(queryStart + 1);
                        tab.currentUrl = fallbackUrl;
                        tab.title = "Tìm kiếm trên web";
                        tab.status = "Google không phản hồi, đang thử Bing...";
                        if (activeTab == tab) statusLabel.setText(tab.status);
                        source.loadURL(fallbackUrl);
                        return;
                    }
                    tab.status = "Không tải được trang: " + errorText;
                    if (activeTab == tab) {
                        statusLabel.setForeground(ERROR);
                        statusLabel.setText(tab.status);
                        progressBar.setVisible(false);
                    }
                });
            }
        });
        tab.client.addFocusHandler(new CefFocusHandlerAdapter() {
            @Override public void onGotFocus(CefBrowser source) {
                if (!addressField.isFocusOwner()) {
                    source.setFocus(true);
                }
            }
        });
        tab.client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override public boolean onBeforePopup(CefBrowser source, CefFrame frame, String targetUrl, String targetFrameName) {
                String popupUrl = targetUrl == null || targetUrl.isBlank() ? "about:blank" : targetUrl;
                SwingUtilities.invokeLater(() -> addBrowserTab(popupUrl, true, false, false));
                return true;
            }
        });
    }

    private void navigateAddress(String input) {
        if (input == null || input.isBlank()) return;
        navigateInTab(activeTab, input, true);
    }

    private void navigateInTab(BrowserTab tab, String input, boolean keepAddressFocus) {
        if (tab == null || input == null || input.isBlank()) return;
        try {
            String resolved = NavigationInput.resolve(input);
            tab.searchFallbackAttempted = false;
            tab.isHome = false;
            tab.currentUrl = resolved;
            tab.title = safeHostTitle(resolved);
            tab.localLayout.show(tab.localCards, "browser");
            tab.browser.loadURL(resolved);
            if (activeTab == tab) {
                addressField.setText(resolved);
                statusLabel.setForeground(MUTED);
                statusLabel.setText(isSearchUrl(resolved) ? "Đang tìm kiếm trên web..." : "Đang mở trang...");
                progressBar.setVisible(true);
                if (keepAddressFocus) {
                    SwingUtilities.invokeLater(this::focusAddressField);
                }
            }
            refreshTabSurfaces();
            if (!keepAddressFocus) {
                SwingUtilities.invokeLater(() -> {
                    if (activeTab == tab && !addressField.hasFocus()) tab.browser.setFocus(true);
                });
            }
        } catch (IllegalArgumentException ex) {
            if (activeTab == tab) {
                statusLabel.setForeground(ERROR);
                statusLabel.setText(ex.getMessage());
            }
        }
    }

    private void selectTab(BrowserTab tab, boolean focusAddress) {
        if (tab == null || !tabs.contains(tab)) return;
        activeTab = tab;
        ((CardLayout) workspace.getLayout()).show(workspace, tab.cardKey);
        addressField.setText(tab.isHome ? "" : (tab.currentUrl == null ? "" : tab.currentUrl));
        statusLabel.setForeground(tab.status.startsWith("Không tải") ? ERROR : MUTED);
        statusLabel.setText(tab.isHome ? "Sẵn sàng khám phá · Chọn lối tắt hoặc tìm kiếm trên web" : (tab.loading ? "Đang tải nội dung..." : tab.status));
        progressBar.setVisible(tab.loading && !tab.isHome);
        reloadButton.setText(tab.loading ? "×" : "↻");
        refreshNavigationButtons();
        refreshTabSurfaces();
        if (focusAddress) {
            SwingUtilities.invokeLater(() -> {
                addressField.requestFocusInWindow();
                addressField.selectAll();
            });
        } else if (!tab.isHome) {
            tab.browser.setFocus(true);
        }
    }

    private void closeTab(BrowserTab tab) {
        int index = tabs.indexOf(tab);
        if (index < 0) return;
        boolean wasActive = activeTab == tab;
        tabs.remove(tab);
        workspace.remove(tab.localCards);
        try { tab.browser.close(true); } catch (Exception ignored) { }
        try { tab.client.dispose(); } catch (Exception ignored) { }
        homePages.remove(tab.homePage);
        if (tabs.isEmpty()) addBrowserTab("", true, true, true);
        else if (wasActive) selectTab(tabs.get(Math.min(index, tabs.size() - 1)), false);
        rebuildRailTabs();
        rebuildExpandedTabs();
        workspace.revalidate();
        workspace.repaint();
    }

    private void rebuildRailTabs() {
        railTabList.removeAll();
        railTabList.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 8));
        for (BrowserTab tab : tabs) {
            ActionButton tabButton = new ActionButton(tab.isHome ? "⌂" : iconFor(tab.title), "Chuyển tới: " + tab.title, tab == activeTab);
            tabButton.setPreferredSize(new Dimension(46, 44));
            tabButton.setMaximumSize(new Dimension(46, 44));
            tabButton.setMinimumSize(new Dimension(46, 44));
            tabButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            tabButton.addActionListener(e -> selectTab(tab, false));
            tab.railButton = tabButton;
            railTabList.add(tabButton);
            railTabList.add(Box.createVerticalStrut(8));
            installHoverTracking(tabButton);
        }
        railTabList.revalidate();
        railTabList.repaint();
    }

    private void rebuildExpandedTabs() {
        expandedTabList.removeAll();
        expandedTabList.setBorder(BorderFactory.createEmptyBorder(4, 0, 3, 0));
        for (BrowserTab tab : tabs) {
            JPanel item = new JPanel(new BorderLayout(10, 0)) {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color top = tab == activeTab ? new Color(79, 55, 177, 215) : new Color(17, 29, 64, 235);
                    Color bottom = tab == activeTab ? new Color(30, 65, 146, 230) : new Color(10, 20, 46, 235);
                    g.setPaint(new GradientPaint(0, 0, top, getWidth(), getHeight(), bottom));
                    g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 17, 17));
                    g.setColor(tab == activeTab ? new Color(139, 101, 255) : new Color(65, 85, 143, 190));
                    g.draw(new RoundRectangle2D.Float(.5f, .5f, getWidth() - 2, getHeight() - 2, 17, 17));
                    g.dispose();
                    super.paintComponent(graphics);
                }
            };
            item.setOpaque(false);
            item.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 6));
            item.setPreferredSize(new Dimension(220, 61));
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 61));
            JLabel glyph = new JLabel(iconFor(tab.isHome ? "Trang chủ" : tab.title), SwingConstants.CENTER);
            glyph.setForeground(Color.WHITE);
            glyph.setFont(new Font("Segoe UI Symbol", Font.BOLD, 19));
            glyph.setOpaque(true);
            glyph.setBackground(new Color(106, 76, 255, 150));
            glyph.setPreferredSize(new Dimension(38, 38));
            glyph.setBorder(BorderFactory.createLineBorder(new Color(125, 144, 255, 130), 1, true));
            item.add(glyph, BorderLayout.WEST);
            JPanel labels = new JPanel(new BorderLayout(0, 4));
            labels.setOpaque(false);
            JLabel title = new JLabel(tab.isHome ? "Trang chủ" : tab.title);
            title.setForeground(TEXT);
            title.setFont(new Font("Segoe UI", Font.BOLD, 12));
            title.setToolTipText(title.getText());
            JLabel subtitle = new JLabel(tab.isHome ? "Explore beyond the ordinary" : shortHost(tab.currentUrl));
            subtitle.setForeground(new Color(177, 194, 229));
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            labels.add(title, BorderLayout.NORTH);
            labels.add(subtitle, BorderLayout.SOUTH);
            item.add(labels, BorderLayout.CENTER);
            ActionButton close = new ActionButton("×", "Đóng tab", false);
            close.setPreferredSize(new Dimension(24, 30));
            close.addActionListener(e -> closeTab(tab));
            item.add(close, BorderLayout.EAST);
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter selectItem = new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { selectTab(tab, false); }
                @Override public void mouseEntered(MouseEvent e) { item.repaint(); }
            };
            item.addMouseListener(selectItem);
            glyph.addMouseListener(selectItem);
            title.addMouseListener(selectItem);
            subtitle.addMouseListener(selectItem);
            labels.addMouseListener(selectItem);
            expandedTabList.add(item);
            expandedTabList.add(Box.createVerticalStrut(7));
            installHoverTracking(item);
        }
        expandedTabList.revalidate();
        expandedTabList.repaint();
    }

    private void refreshTabSurfaces() {
        rebuildRailTabs();
        rebuildExpandedTabs();
        if (activeTab != null) {
            backButton.setEnabled(activeTab.canGoBack && !activeTab.isHome);
            forwardButton.setEnabled(activeTab.canGoForward && !activeTab.isHome);
            bookmarkButton.setText(bookmarks.containsKey(activeTab.currentUrl) ? "★" : "☆");
        }
    }

    private void refreshNavigationButtons() {
        backButton.setEnabled(activeTab != null && activeTab.canGoBack && !activeTab.isHome);
        forwardButton.setEnabled(activeTab != null && activeTab.canGoForward && !activeTab.isHome);
    }

    private void toggleBookmark() {
        if (activeTab == null || activeTab.isHome || activeTab.currentUrl == null || activeTab.currentUrl.isBlank()) {
            statusLabel.setText("Mở một trang web trước khi lưu dấu trang.");
            return;
        }
        if (bookmarks.containsKey(activeTab.currentUrl)) {
            bookmarks.remove(activeTab.currentUrl);
            statusLabel.setText("Đã bỏ dấu trang.");
        } else {
            bookmarks.put(activeTab.currentUrl, activeTab.title);
            statusLabel.setText("Đã lưu dấu trang: " + activeTab.title);
        }
        bookmarkButton.setText(bookmarks.containsKey(activeTab.currentUrl) ? "★" : "☆");
    }

    private void setExpandedVisible(boolean visible) {
        expandedByPointer = visible;
        expandedTabs.setVisible(visible);
        if (visible) layers.moveToFront(expandedTabs);
        layers.repaint();
    }

    private void installHoverTracking(Component component) {
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { setExpandedVisible(true); }
            @Override public void mouseExited(MouseEvent e) { SwingUtilities.invokeLater(BrowserPanel.this::updateExpandedFromPointer); }
        });
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) installHoverTracking(child);
        }
    }

    private void updateExpandedFromPointer() {
        if (!isShowing()) return;
        try {
            Point screen = java.awt.MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(screen, layers);
            boolean overRail = rail.getBounds().contains(screen);
            boolean overPanel = expandedTabs.isVisible() && expandedTabs.getBounds().contains(screen);
            if (!overRail && !overPanel) setExpandedVisible(false);
        } catch (Exception ignored) {
            setExpandedVisible(false);
        }
    }

    private static String iconFor(String title) {
        String value = title == null ? "" : title.toLowerCase();
        if (value.contains("shrimple") || value.contains("trang chủ")) return "⬡";
        if (value.contains("github")) return "◉";
        if (value.contains("java")) return "♨";
        if (value.contains("youtube")) return "▶";
        if (value.contains("google")) return "G";
        return value.isBlank() ? "◈" : String.valueOf(title.trim().charAt(0)).toUpperCase();
    }

    private static boolean isSearchUrl(String url) {
        return url != null && (url.startsWith("https://www.google.com/search?q=")
                || url.startsWith("https://www.bing.com/search?q="));
    }

    private static String safeHostTitle(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? url : uri.getHost().replaceFirst("^www\\.", "");
        } catch (Exception ignored) { return url; }
    }

    private static String shortHost(String url) {
        if (url == null || url.isBlank() || url.startsWith("npt:")) return "Trang chủ";
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? url : uri.getHost();
        } catch (Exception ignored) { return url; }
    }

    private void withActive(java.util.function.Consumer<BrowserTab> action) {
        if (activeTab != null) action.accept(activeTab);
    }

    public void goHome() {
        withActive(tab -> {
            tab.isHome = true;
            tab.currentUrl = "npt://home";
            tab.title = "Trang chủ";
            tab.localLayout.show(tab.localCards, "home");
            if (activeTab == tab) {
                addressField.setText("");
                statusLabel.setForeground(MUTED);
                statusLabel.setText("Sẵn sàng khám phá · Chọn lối tắt hoặc tìm kiếm trên web");
                progressBar.setVisible(false);
            }
            refreshTabSurfaces();
        });
    }

    public void newTab() { addBrowserTab("", true, true, true); }
    public void closeActiveTab() { if (activeTab != null) closeTab(activeTab); }
    public void reloadActiveTab() {
        if (activeTab != null && !activeTab.isHome) {
            if (activeTab.loading) activeTab.browser.stopLoad(); else activeTab.browser.reload();
        }
    }
    public void nextTab() {
        if (tabs.size() > 1 && activeTab != null) selectTab(tabs.get((tabs.indexOf(activeTab) + 1) % tabs.size()), false);
    }
    public void focusAddress() {
        SwingUtilities.invokeLater(this::focusAddressField);
    }

    private void focusAddressField() {
        addressField.requestFocusInWindow();
        addressField.selectAll();
    }

    private void refreshAllHomeShortcuts() {
        for (HomePage page : new ArrayList<>(homePages)) page.refreshShortcuts();
    }

    private void addQuickLinkFromDialog() {
        if (quickLinkStore == null) {
            JOptionPane.showMessageDialog(this, "Không mở được kho lối tắt. Kiểm tra thư mục config.", "NPT Browser", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JTextField titleField = new JTextField(24);
        JTextField urlField = new JTextField(24);
        titleField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        urlField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JPanel fields = new JPanel(new java.awt.GridLayout(0, 1, 5, 5));
        fields.add(new JLabel("Tên trang web"));
        fields.add(titleField);
        fields.add(new JLabel("Địa chỉ URL"));
        fields.add(urlField);
        int result = JOptionPane.showConfirmDialog(this, fields, "Thêm lối tắt", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        try {
            String rawUrl = urlField.getText().trim();
            if (rawUrl.isBlank()) throw new IllegalArgumentException("Hãy nhập địa chỉ trang web.");
            String resolved = NavigationInput.resolve(rawUrl);
            if (!(resolved.startsWith("http://") || resolved.startsWith("https://")) || isSearchUrl(resolved)) {
                throw new IllegalArgumentException("Địa chỉ lối tắt phải là URL hoặc tên miền, không phải từ khóa tìm kiếm.");
            }
            String title = titleField.getText().trim();
            if (title.isBlank()) title = safeHostTitle(resolved);
            quickLinkStore.add(new QuickLink(title, resolved, "◈", "7246FF"));
            refreshAllHomeShortcuts();
            statusLabel.setForeground(MUTED);
            statusLabel.setText("Đã thêm lối tắt: " + title);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Không thể thêm lối tắt", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void removeQuickLink(QuickLink link) {
        if (quickLinkStore == null) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Xóa lối tắt ‘" + link.title() + "’?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            quickLinkStore.remove(link);
            refreshAllHomeShortcuts();
            statusLabel.setForeground(MUTED);
            statusLabel.setText("Đã xóa lối tắt: " + link.title());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được thay đổi: " + ex.getMessage(), "Lỗi lưu lối tắt", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void close() {
        for (BrowserTab tab : new ArrayList<>(tabs)) {
            try { tab.browser.close(true); } catch (Exception ignored) { }
            try { tab.client.dispose(); } catch (Exception ignored) { }
        }
        tabs.clear();
    }

    private final class BrowserTab {
        private final int number;
        private final String cardKey;
        private CefClient client;
        private CefBrowser browser;
        private Component browserComponent;
        private JPanel localCards;
        private CardLayout localLayout;
        private HomePage homePage;
        private ActionButton railButton;
        private String title = "Trang chủ";
        private String currentUrl = "npt://home";
        private String status = "Sẵn sàng";
        private boolean isHome = true;
        private boolean loading;
        private boolean canGoBack;
        private boolean canGoForward;
        private boolean searchFallbackAttempted;
        private BrowserTab(int number) { this.number = number; this.cardKey = "browser-tab-" + number; }
    }

    @FunctionalInterface private interface NavigateHandler { void navigate(String destination); }

    private static final class HomePage extends JPanel {
        private static final Image BACKGROUND = loadBackground();
        private final JTextField searchField = new PlaceholderField("Tìm kiếm hoặc nhập địa chỉ web");
        private final QuickLinkStore quickLinkStore;
        private final NavigateHandler navigate;
        private final Runnable addLinkAction;
        private final java.util.function.Consumer<QuickLink> removeLinkAction;
        private final JPanel shortcuts = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        private final JScrollPane shortcutsScroll = new JScrollPane(shortcuts,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        private HomePage(QuickLinkStore quickLinkStore, NavigateHandler navigate,
                         Runnable addLinkAction, java.util.function.Consumer<QuickLink> removeLinkAction) {
            super(new BorderLayout());
            this.quickLinkStore = quickLinkStore;
            this.navigate = navigate;
            this.addLinkAction = addLinkAction;
            this.removeLinkAction = removeLinkAction;
            setOpaque(true);
            setBackground(INK);
            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setBorder(BorderFactory.createEmptyBorder(12, 24, 5, 24));
            center.add(Box.createVerticalGlue());

            LogoMark logo = new LogoMark();
            logo.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(logo);
            center.add(Box.createVerticalStrut(1));
            GradientTitle title = new GradientTitle("NPT BROWSER");
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(title);
            JLabel tagline = new JLabel("E X P L O R E   B E Y O N D   T H E   O R D I N A R Y", SwingConstants.CENTER);
            tagline.setForeground(new Color(194, 211, 255));
            tagline.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(tagline);
            center.add(Box.createVerticalStrut(31));

            JPanel search = buildHomeSearch(navigate);
            search.setAlignmentX(Component.CENTER_ALIGNMENT);
            search.setMaximumSize(new Dimension(760, 62));
            center.add(search);
            center.add(Box.createVerticalStrut(25));

            shortcuts.setOpaque(false);
            refreshShortcuts();
            shortcuts.setAlignmentX(Component.CENTER_ALIGNMENT);
            shortcutsScroll.setOpaque(false);
            shortcutsScroll.getViewport().setOpaque(false);
            shortcutsScroll.setBorder(BorderFactory.createEmptyBorder());
            shortcutsScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 7));
            shortcutsScroll.setPreferredSize(new Dimension(780, 169));
            shortcutsScroll.setMaximumSize(new Dimension(1040, 169));
            shortcutsScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(shortcutsScroll);
            center.add(Box.createVerticalGlue());
            add(center, BorderLayout.CENTER);

            JPanel quote = new JPanel(new FlowLayout(FlowLayout.LEFT, 13, 10));
            quote.setOpaque(false);
            quote.setBorder(BorderFactory.createEmptyBorder(0, 20, 13, 10));
            JLabel orbit = new JLabel("◉");
            orbit.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 22));
            orbit.setForeground(new Color(144, 130, 255));
            quote.add(orbit);
            JPanel quoteWords = new JPanel(new BorderLayout(0, 3));
            quoteWords.setOpaque(false);
            JLabel phrase = new JLabel("Công nghệ mở ra những chân trời mới");
            phrase.setForeground(new Color(226, 234, 255));
            phrase.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            JLabel sub = new JLabel("NPT BROWSER  ·  SPACE EDITION");
            sub.setForeground(new Color(130, 159, 220));
            sub.setFont(new Font("Segoe UI", Font.BOLD, 8));
            quoteWords.add(phrase, BorderLayout.NORTH);
            quoteWords.add(sub, BorderLayout.SOUTH);
            quote.add(quoteWords);
            add(quote, BorderLayout.SOUTH);
        }

        private void refreshShortcuts() {
            shortcuts.removeAll();
            List<QuickLink> links = quickLinkStore == null
                    ? QuickLinkStore.defaults("http://NPT-SERVER/") : quickLinkStore.list();
            for (QuickLink link : links) {
                Color glow = colorFromHex(link.color());
                shortcuts.add(new ShortcutTile(link.glyph(), link.title(), shortDisplay(link.url()), glow,
                        () -> navigate.navigate(link.url()), () -> removeLinkAction.accept(link)));
            }
            shortcuts.add(new ShortcutTile("+", "Thêm lối tắt", "Thêm trang web", new Color(39, 214, 255),
                    addLinkAction, null));
            shortcuts.revalidate();
            shortcuts.repaint();
        }

        private static Color colorFromHex(String value) {
            try { return new Color(Integer.parseInt(value.replace("#", ""), 16)); }
            catch (Exception ignored) { return new Color(114, 70, 255); }
        }

        private JPanel buildHomeSearch(NavigateHandler navigate) {
            JPanel search = new JPanel(new BorderLayout(10, 0)) {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setPaint(new GradientPaint(0, 0, new Color(8, 20, 58, 225), getWidth(), getHeight(), new Color(12, 17, 56, 238)));
                    g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 28, 28));
                    g.setPaint(new GradientPaint(0, 0, CYAN, getWidth(), getHeight(), new Color(155, 79, 255)));
                    g.draw(new RoundRectangle2D.Float(.7f, .7f, getWidth() - 2, getHeight() - 2, 28, 28));
                    g.dispose();
                    super.paintComponent(graphics);
                }
            };
            search.setOpaque(false);
            search.setPreferredSize(new Dimension(720, 62));
            search.setBorder(BorderFactory.createEmptyBorder(7, 17, 7, 8));
            JLabel glass = new JLabel("⌕");
            glass.setForeground(Color.WHITE);
            glass.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 27));
            search.add(glass, BorderLayout.WEST);
            searchField.setOpaque(false);
            searchField.setForeground(TEXT);
            searchField.setCaretColor(CYAN);
            searchField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            searchField.setHorizontalAlignment(SwingConstants.CENTER);
            searchField.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 0));
            searchField.addActionListener(e -> navigate.navigate(searchField.getText()));
            search.add(searchField, BorderLayout.CENTER);
            ActionButton go = new ActionButton("➜", "Tìm kiếm / mở trang", true);
            go.setPreferredSize(new Dimension(42, 42));
            go.addActionListener(e -> navigate.navigate(searchField.getText()));
            search.add(go, BorderLayout.EAST);
            return search;
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if (BACKGROUND != null) {
                double scale = Math.max((double) getWidth() / BACKGROUND.getWidth(null), (double) getHeight() / BACKGROUND.getHeight(null));
                int width = (int) Math.ceil(BACKGROUND.getWidth(null) * scale);
                int height = (int) Math.ceil(BACKGROUND.getHeight(null) * scale);
                g.drawImage(BACKGROUND, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
            } else {
                g.setPaint(new GradientPaint(0, 0, new Color(6, 17, 51), getWidth(), getHeight(), new Color(33, 14, 83)));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            g.setComposite(AlphaComposite.SrcOver.derive(0.13f));
            g.setColor(new Color(3, 8, 30));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }

        private static Image loadBackground() {
            try { return ImageIO.read(HomePage.class.getResourceAsStream("/space-background.jpg")); }
            catch (IOException | IllegalArgumentException ex) { return null; }
        }

        private static String shortDisplay(String url) {
            try { URI uri = URI.create(url); return uri.getHost() == null ? url : uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : ""); }
            catch (Exception ignored) { return url; }
        }
    }

    private static final class LogoMark extends JPanel {
        private LogoMark() { setOpaque(false); setPreferredSize(new Dimension(118, 105)); setMaximumSize(new Dimension(118, 105)); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            g.setColor(new Color(0, 171, 255, 35));
            g.fill(new Ellipse2D.Double(cx - 46, cy - 40, 92, 80));
            g.rotate(-0.23, cx, cy);
            g.setStroke(new java.awt.BasicStroke(4.3f));
            g.setPaint(new GradientPaint(cx - 49, cy, new Color(36, 233, 255), cx + 48, cy, new Color(133, 76, 255)));
            g.drawOval(cx - 51, cy - 22, 102, 45);
            g.rotate(0.23, cx, cy);
            g.setFont(new Font("Segoe UI", Font.BOLD, 78));
            g.setPaint(new GradientPaint(cx - 36, cy - 37, new Color(0, 226, 255), cx + 34, cy + 34, new Color(135, 74, 255)));
            String n = "N";
            int width = g.getFontMetrics().stringWidth(n);
            g.drawString(n, cx - width / 2, cy + 27);
            g.dispose();
        }
    }

    private static final class GradientTitle extends JLabel {
        private GradientTitle(String text) { super(text, SwingConstants.CENTER); setFont(new Font("Segoe UI", Font.BOLD, 38)); setPreferredSize(new Dimension(600, 51)); setMaximumSize(new Dimension(740, 51)); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font font = getFont();
            g.setFont(font);
            java.awt.FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g.setColor(new Color(28, 127, 255, 130));
            g.drawString(getText(), x + 1, y + 2);
            g.setPaint(new GradientPaint(x, y - 30, Color.WHITE, x + fm.stringWidth(getText()), y, new Color(149, 180, 255)));
            g.drawString(getText(), x, y);
            g.dispose();
        }
    }

    private static final class ShortcutTile extends JPanel {
        private boolean hovering;
        private final Runnable action;

        private ShortcutTile(String glyphText, String titleText, String host, Color glow, Runnable action, Runnable removeAction) {
            this.action = action;
            setOpaque(false);
            setPreferredSize(new Dimension(142, 151));
            setMinimumSize(new Dimension(130, 145));
            setMaximumSize(new Dimension(158, 151));
            setBorder(BorderFactory.createEmptyBorder(6, 8, 11, 8));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.setPreferredSize(new Dimension(124, 20));
            top.setMaximumSize(new Dimension(124, 20));
            top.setMinimumSize(new Dimension(124, 20));
            if (removeAction != null) {
                JButton remove = new JButton("×");
                remove.setToolTipText("Xóa lối tắt");
                remove.setFocusable(false);
                remove.setMargin(new Insets(0, 0, 0, 0));
                remove.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
                remove.setForeground(new Color(211, 221, 255));
                remove.setOpaque(false);
                remove.setContentAreaFilled(false);
                remove.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                remove.addActionListener(e -> removeAction.run());
                top.add(remove, BorderLayout.EAST);
            }
            add(top);
            add(Box.createVerticalStrut(4));

            JLabel glyph = new JLabel(glyphText, SwingConstants.CENTER);
            glyph.setOpaque(true);
            glyph.setBackground(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 85));
            glyph.setForeground(titleText.equalsIgnoreCase("YouTube") ? new Color(255, 75, 98)
                    : titleText.toLowerCase().contains("java") ? new Color(255, 139, 86) : Color.WHITE);
            glyph.setFont(new Font("Segoe UI Symbol", Font.BOLD, 29));
            glyph.setHorizontalAlignment(SwingConstants.CENTER);
            glyph.setVerticalAlignment(SwingConstants.CENTER);
            glyph.setBorder(BorderFactory.createLineBorder(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 140), 1, true));
            glyph.setPreferredSize(new Dimension(56, 56));
            glyph.setMinimumSize(new Dimension(56, 56));
            glyph.setMaximumSize(new Dimension(56, 56));
            glyph.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(glyph);
            add(Box.createVerticalStrut(9));

            JLabel title = new JLabel(titleText, SwingConstants.CENTER);
            title.setForeground(Color.WHITE);
            title.setFont(new Font("Segoe UI", Font.BOLD, 12));
            title.setHorizontalAlignment(SwingConstants.CENTER);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(title);
            add(Box.createVerticalStrut(5));

            JLabel detail = new JLabel(host, SwingConstants.CENTER);
            detail.setForeground(new Color(190, 205, 235));
            detail.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            detail.setHorizontalAlignment(SwingConstants.CENTER);
            detail.setAlignmentX(Component.CENTER_ALIGNMENT);
            detail.setToolTipText(host);
            add(detail);

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter listener = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovering = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { if (ShortcutTile.this.action != null && SwingUtilities.isLeftMouseButton(e)) ShortcutTile.this.action.run(); }
            };
            addMouseListener(listener);
            glyph.addMouseListener(listener);
            title.addMouseListener(listener);
            detail.addMouseListener(listener);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, hovering ? new Color(42, 60, 135, 235) : new Color(12, 23, 59, 225), getWidth(), getHeight(), new Color(5, 12, 36, 235)));
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 23, 23));
            g.setPaint(new GradientPaint(0, 0, hovering ? new Color(61, 219, 255, 245) : new Color(77, 112, 218, 205), getWidth(), getHeight(), new Color(129, 75, 255, 210)));
            g.draw(new RoundRectangle2D.Float(.6f, .6f, getWidth() - 2, getHeight() - 2, 23, 23));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class PlaceholderField extends JTextField {
        private final String placeholder;
        private PlaceholderField(String placeholder) { this.placeholder = placeholder; }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty() && !hasFocus()) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(getFont());
                g.setColor(new Color(146, 165, 207));
                Insets insets = getInsets();
                int textWidth = g.getFontMetrics().stringWidth(placeholder);
                int x = switch (getHorizontalAlignment()) {
                    case SwingConstants.CENTER -> (getWidth() - textWidth) / 2;
                    case SwingConstants.RIGHT, SwingConstants.TRAILING -> getWidth() - insets.right - textWidth;
                    default -> insets.left + 1;
                };
                int baseline = (getHeight() - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent();
                g.drawString(placeholder, x, baseline);
                g.dispose();
            }
        }
    }

    private static final class ActionButton extends JButton {
        private final boolean accent;
        private ActionButton(String text, String tooltip, boolean accent) {
            super(text); this.accent = accent;
            setToolTipText(tooltip);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pad = accent ? 0 : 2;
            if (accent) {
                g.setPaint(new GradientPaint(0, 0, new Color(137, 74, 255), getWidth(), getHeight(), new Color(22, 136, 255)));
                g.fill(new RoundRectangle2D.Float(pad, pad, getWidth() - 2 * pad - 1, getHeight() - 2 * pad - 1, 18, 18));
                g.setColor(new Color(112, 194, 255, 180));
                g.draw(new RoundRectangle2D.Float(pad + .5f, pad + .5f, getWidth() - 2 * pad - 2, getHeight() - 2 * pad - 2, 18, 18));
            } else {
                Color bg = getModel().isPressed() ? new Color(92, 64, 191, 230) : getModel().isRollover() ? new Color(52, 69, 126, 210) : new Color(20, 33, 69, 175);
                g.setColor(bg);
                g.fill(new RoundRectangle2D.Float(pad, pad, getWidth() - 2 * pad - 1, getHeight() - 2 * pad - 1, 16, 16));
                g.setColor(new Color(83, 109, 178, 105));
                g.draw(new RoundRectangle2D.Float(pad + .5f, pad + .5f, getWidth() - 2 * pad - 2, getHeight() - 2 * pad - 2, 16, 16));
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}

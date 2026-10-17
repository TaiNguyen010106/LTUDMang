package vn.edu.nptbrowser.ui;

import org.cef.CefApp;
import vn.edu.nptbrowser.browser.BrowserPanel;
import vn.edu.nptbrowser.config.AppConfig;
import vn.edu.nptbrowser.http.HttpTesterPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
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
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;

public final class BrowserFrame extends JFrame {
    private static final Color INK = new Color(5, 9, 23);
    private static final Color TEXT = new Color(241, 246, 255);
    private static final Color MUTED = new Color(160, 180, 220);
    private final CefApp cefApp;
    private final AppConfig config;
    private final BrowserPanel browserPanel;
    private final HttpTesterPanel testerPanel;
    private final JPanel mainPages = new JPanel(new CardLayout());
    private final ModeButton browserMode = new ModeButton("◈  TRÌNH DUYỆT");
    private final ModeButton testerMode = new ModeButton("⚡  HTTP TESTER");
    private JPanel outer;
    private JPanel header;
    private Rectangle normalBounds;
    private boolean maximized;
    private Point dragStart;
    private Rectangle dragBounds;
    private int resizeEdges;
    private KeyEventDispatcher shortcutDispatcher;
    private static final int EDGE = 7;
    private static final int LEFT = 1, RIGHT = 2, TOP = 4, BOTTOM = 8;

    public BrowserFrame(CefApp cefApp, AppConfig config) {
        super("NPT Browser — Space Edition");
        this.cefApp = cefApp;
        this.config = config;
        setUndecorated(true);
        this.browserPanel = new BrowserPanel(cefApp, config, this::openSettings);
        this.testerPanel = new HttpTesterPanel(config);
        buildUi();
        installShortcuts();
    }

    private void buildUi() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 690));
        setSize(1450, 900);
        outer = new JPanel(new BorderLayout());
        outer.setBackground(new Color(6, 11, 30));
        outer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(INK);
        shell.setBorder(BorderFactory.createLineBorder(new Color(44, 70, 149), 1));
        outer.add(shell, BorderLayout.CENTER);
        setContentPane(outer);

        header = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, new Color(5, 11, 30), getWidth() * .52f, getHeight(), new Color(19, 26, 73)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(76, 91, 176, 130));
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 7));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        JPanel logo = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, new Color(157, 66, 255), getWidth(), getHeight(), new Color(10, 211, 255)));
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
                g.setColor(new Color(255, 255, 255, 120));
                g.draw(new RoundRectangle2D.Float(.5f, .5f, getWidth() - 1, getHeight() - 1, 15, 15));
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(35, 35));
        JLabel n = new JLabel("N", SwingConstants.CENTER);
        n.setForeground(Color.WHITE);
        n.setFont(new Font("Segoe UI", Font.BOLD, 23));
        logo.add(n, BorderLayout.CENTER);
        brand.add(logo);
        JPanel brandText = new JPanel(new BorderLayout(0, 1));
        brandText.setOpaque(false);
        JLabel title = new JLabel("NPT BROWSER");
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel subtitle = new JLabel("EXPLORE BEYOND THE ORDINARY");
        subtitle.setForeground(new Color(141, 169, 231));
        subtitle.setFont(new Font("Segoe UI", Font.BOLD, 8));
        brandText.add(title, BorderLayout.CENTER);
        brandText.add(subtitle, BorderLayout.SOUTH);
        brand.add(brandText);
        header.add(brand, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        headerRight.setOpaque(false);
        browserMode.addActionListener(e -> showMode("browser"));
        testerMode.addActionListener(e -> showMode("tester"));
        headerRight.add(browserMode);
        headerRight.add(testerMode);
        headerRight.add(windowButton("—", "Thu nhỏ", () -> setState(JFrame.ICONIFIED), false));
        headerRight.add(windowButton("□", "Phóng to / khôi phục", this::toggleMaximize, false));
        headerRight.add(windowButton("×", "Đóng NPT Browser", this::closeApp, true));
        header.add(headerRight, BorderLayout.EAST);
        shell.add(header, BorderLayout.NORTH);

        mainPages.setOpaque(false);
        mainPages.add(browserPanel, "browser");
        mainPages.add(testerPanel, "tester");
        shell.add(mainPages, BorderLayout.CENTER);
        showMode("browser");

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { closeApp(); }
        });
        installWindowDraggingAndResizing();
        setLocationRelativeTo(null);
    }

    private JButton windowButton(String text, String tooltip, Runnable action, boolean close) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover() || getModel().isPressed()) {
                    g.setColor(close ? new Color(222, 57, 91, 220) : new Color(75, 92, 151, 200));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setToolTipText(tooltip);
        button.setForeground(close ? new Color(255, 174, 194) : new Color(218, 229, 255));
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, close ? 21 : 16));
        button.setPreferredSize(new Dimension(38, 34));
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        return button;
    }

    private void showMode(String card) {
        ((CardLayout) mainPages.getLayout()).show(mainPages, card);
        browserMode.setSelectedMode("browser".equals(card));
        testerMode.setSelectedMode("tester".equals(card));
    }

    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this, config);
        dialog.setVisible(true);
        if (dialog.wasSaved()) {
            browserPanel.goHome();
            testerPanel.refreshApiBaseUrl();
        }
    }

    private void closeApp() {
        try { browserPanel.close(); } catch (Exception ignored) { }
        try { cefApp.dispose(); } catch (Exception ignored) { }
        dispose();
    }

    private void toggleMaximize() {
        if (!maximized) {
            normalBounds = getBounds();
            setBounds(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
            maximized = true;
        } else {
            if (normalBounds != null) setBounds(normalBounds);
            maximized = false;
        }
    }

    private void installWindowDraggingAndResizing() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int edges = getResizeEdges(e.getPoint());
                outer.setCursor(Cursor.getPredefinedCursor(cursorFor(edges)));
            }
            @Override public void mouseExited(MouseEvent e) { if (dragStart == null) outer.setCursor(Cursor.getDefaultCursor()); }
            @Override public void mousePressed(MouseEvent e) {
                Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), outer);
                resizeEdges = getResizeEdges(p);
                if (resizeEdges != 0 && !maximized) {
                    dragStart = e.getLocationOnScreen();
                    dragBounds = getBounds();
                } else if (e.getY() <= 44 && e.getSource() == outer) {
                    dragStart = e.getLocationOnScreen();
                    dragBounds = getBounds();
                    resizeEdges = 0;
                }
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragStart == null) return;
                Point now = e.getLocationOnScreen();
                int dx = now.x - dragStart.x;
                int dy = now.y - dragStart.y;
                if (resizeEdges == 0) {
                    setLocation(dragBounds.x + dx, dragBounds.y + dy);
                    return;
                }
                int x = dragBounds.x, y = dragBounds.y, w = dragBounds.width, h = dragBounds.height;
                if ((resizeEdges & LEFT) != 0) { x += dx; w -= dx; }
                if ((resizeEdges & RIGHT) != 0) w += dx;
                if ((resizeEdges & TOP) != 0) { y += dy; h -= dy; }
                if ((resizeEdges & BOTTOM) != 0) h += dy;
                if (w < getMinimumSize().width) { if ((resizeEdges & LEFT) != 0) x = dragBounds.x + dragBounds.width - getMinimumSize().width; w = getMinimumSize().width; }
                if (h < getMinimumSize().height) { if ((resizeEdges & TOP) != 0) y = dragBounds.y + dragBounds.height - getMinimumSize().height; h = getMinimumSize().height; }
                setBounds(x, y, w, h);
            }
            @Override public void mouseReleased(MouseEvent e) { dragStart = null; dragBounds = null; resizeEdges = 0; }
        };
        outer.addMouseListener(adapter);
        outer.addMouseMotionListener(adapter);
        MouseAdapter dragHeader = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) { toggleMaximize(); return; }
                if (SwingUtilities.isLeftMouseButton(e)) { dragStart = e.getLocationOnScreen(); dragBounds = getBounds(); }
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragStart != null && !maximized) {
                    Point now = e.getLocationOnScreen();
                    setLocation(dragBounds.x + now.x - dragStart.x, dragBounds.y + now.y - dragStart.y);
                }
            }
            @Override public void mouseReleased(MouseEvent e) { dragStart = null; dragBounds = null; }
        };
        header.addMouseListener(dragHeader);
        header.addMouseMotionListener(dragHeader);
        for (Component child : header.getComponents()) attachDragHandlers(child, dragHeader);
    }

    private static void attachDragHandlers(Component component, MouseAdapter adapter) {
        if (component instanceof JButton) return;
        component.addMouseListener(adapter);
        component.addMouseMotionListener(adapter);
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) attachDragHandlers(child, adapter);
        }
    }

    private int getResizeEdges(Point p) {
        int edges = 0;
        if (p.x <= EDGE) edges |= LEFT;
        else if (p.x >= outer.getWidth() - EDGE) edges |= RIGHT;
        if (p.y <= EDGE) edges |= TOP;
        else if (p.y >= outer.getHeight() - EDGE) edges |= BOTTOM;
        return edges;
    }

    private int cursorFor(int edges) {
        if ((edges & LEFT) != 0 && (edges & TOP) != 0) return Cursor.NW_RESIZE_CURSOR;
        if ((edges & RIGHT) != 0 && (edges & TOP) != 0) return Cursor.NE_RESIZE_CURSOR;
        if ((edges & LEFT) != 0 && (edges & BOTTOM) != 0) return Cursor.SW_RESIZE_CURSOR;
        if ((edges & RIGHT) != 0 && (edges & BOTTOM) != 0) return Cursor.SE_RESIZE_CURSOR;
        if ((edges & LEFT) != 0 || (edges & RIGHT) != 0) return Cursor.E_RESIZE_CURSOR;
        if ((edges & TOP) != 0 || (edges & BOTTOM) != 0) return Cursor.N_RESIZE_CURSOR;
        return Cursor.DEFAULT_CURSOR;
    }

    private void installShortcuts() {
        shortcutDispatcher = event -> {
            if (event.getID() != KeyEvent.KEY_PRESSED
                    || (event.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0) {
                return false;
            }
            switch (event.getKeyCode()) {
                case KeyEvent.VK_T -> {
                    browserPanel.newTab();
                    showMode("browser");
                }
                case KeyEvent.VK_W -> browserPanel.closeActiveTab();
                case KeyEvent.VK_L -> {
                    browserPanel.focusAddress();
                    showMode("browser");
                }
                case KeyEvent.VK_R -> browserPanel.reloadActiveTab();
                case KeyEvent.VK_TAB -> browserPanel.nextTab();
                default -> {
                    return false;
                }
            }
            event.consume();
            return true;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(shortcutDispatcher);
    }

    @Override
    public void dispose() {
        if (shortcutDispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(shortcutDispatcher);
            shortcutDispatcher = null;
        }
        super.dispose();
    }

    private static final class ModeButton extends JButton {
        private boolean selectedMode;
        private ModeButton(String text) {
            super(text);
            setForeground(TEXT);
            setFont(new Font("Segoe UI", Font.BOLD, 10));
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            setContentAreaFilled(false);
            setOpaque(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        private void setSelectedMode(boolean selected) { selectedMode = selected; repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selectedMode || getModel().isRollover()) {
                g.setPaint(new GradientPaint(0, 0, new Color(116, 63, 255, 230), getWidth(), getHeight(), new Color(24, 116, 237, 230)));
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 13, 13));
                g.setColor(new Color(144, 175, 255, 170));
                g.draw(new RoundRectangle2D.Float(.5f, .5f, getWidth() - 2, getHeight() - 2, 13, 13));
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}

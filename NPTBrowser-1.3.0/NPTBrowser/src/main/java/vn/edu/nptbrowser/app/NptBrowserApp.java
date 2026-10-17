package vn.edu.nptbrowser.app;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import vn.edu.nptbrowser.config.AppConfig;
import vn.edu.nptbrowser.ui.BrowserFrame;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public final class NptBrowserApp {
    private NptBrowserApp() {
    }

    public static void main(String[] args) {
        setSystemLookAndFeel();
        SwingUtilities.invokeLater(() -> start(args));
    }

    private static void start(String[] args) {
        LoadingWindow loading = new LoadingWindow();
        loading.setVisible(true);

        CompletableFuture.supplyAsync(() -> initialize(args, loading))
                .whenComplete((context, error) -> SwingUtilities.invokeLater(() -> {
                    loading.dispose();
                    if (error != null) {
                        Throwable cause = error.getCause() == null ? error : error.getCause();
                        JOptionPane.showMessageDialog(
                                null,
                                "Không khởi động được NPT Browser.\n" + cause.getClass().getSimpleName() + ": " + cause.getMessage(),
                                "Lỗi khởi động",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    BrowserFrame frame = new BrowserFrame(context.cefApp(), context.config());
                    frame.setVisible(true);
                }));
    }

    private static AppContext initialize(String[] args, LoadingWindow loading) {
        try {
            AppConfig config = AppConfig.load();
            Files.createDirectories(config.jcefInstallDir());
            Files.createDirectories(config.browserCacheDir());

            CefAppBuilder builder = new CefAppBuilder();
            builder.setInstallDir(config.jcefInstallDir().toFile());
            builder.getCefSettings().windowless_rendering_enabled = false;
            builder.getCefSettings().cache_path = config.browserCacheDir().toString();
            builder.getCefSettings().locale = "vi-VN";
            // Favor Vietnamese text and UTF-8 when a page omits an explicit charset.
            builder.addJcefArgs("--accept-lang=" + config.acceptLanguage());
            builder.addJcefArgs("--lang=vi");
            builder.addJcefArgs("--default-encoding=UTF-8");
            builder.setProgressHandler((state, percent) -> {
                String suffix = percent >= 0 ? String.format(" %.0f%%", percent) : "";
                loading.updateStatus("JCEF: " + state + suffix, percent);
            });
            builder.setAppHandler(new MavenCefAppHandlerAdapter() {
                @Override
                public void stateHasChanged(CefApp.CefAppState state) {
                    if (state == CefApp.CefAppState.TERMINATED) {
                        System.exit(0);
                    }
                }
            });
            if (args.length > 0) {
                builder.addJcefArgs(args);
            }
            return new AppContext(builder.build(), config);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Swing's cross-platform look and feel remains available.
        }
    }

    private record AppContext(CefApp cefApp, AppConfig config) {
    }

    private static final class LoadingWindow extends JFrame {
        private final JLabel status = new JLabel("Đang khởi tạo Chromium…", SwingConstants.CENTER);
        private final JProgressBar progress = new JProgressBar(0, 100);

        private LoadingWindow() {
            super("NPT Browser · Khởi động");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JPanel content = new JPanel(new BorderLayout(8, 13)) {
                @Override protected void paintComponent(java.awt.Graphics graphics) {
                    java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
                    g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setPaint(new java.awt.GradientPaint(0, 0, new java.awt.Color(18, 22, 55),
                            getWidth(), getHeight(), new java.awt.Color(20, 55, 91)));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.dispose();
                    super.paintComponent(graphics);
                }
            };
            content.setOpaque(false);
            content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

            JLabel title = new JLabel("NPT Browser", SwingConstants.CENTER);
            title.setForeground(java.awt.Color.WHITE);
            title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 27));
            JLabel subtitle = new JLabel("BROWSER STUDIO  ·  CHROMIUM ENGINE", SwingConstants.CENTER);
            subtitle.setForeground(new java.awt.Color(156, 220, 238));
            subtitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 9));
            JPanel heading = new JPanel(new BorderLayout(0, 3));
            heading.setOpaque(false);
            heading.add(title, BorderLayout.CENTER);
            heading.add(subtitle, BorderLayout.SOUTH);
            content.add(heading, BorderLayout.NORTH);

            status.setForeground(new java.awt.Color(220, 231, 249));
            status.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
            content.add(status, BorderLayout.CENTER);
            progress.setIndeterminate(true);
            progress.setBorderPainted(false);
            progress.setForeground(new java.awt.Color(43, 218, 231));
            progress.setBackground(new java.awt.Color(46, 59, 91));
            content.add(progress, BorderLayout.SOUTH);
            setContentPane(content);
            setPreferredSize(new Dimension(500, 205));
            pack();
            setResizable(false);
            setLocationRelativeTo(null);
        }

        private void updateStatus(String text, float percent) {
            SwingUtilities.invokeLater(() -> {
                status.setText(text);
                progress.setIndeterminate(percent < 0);
                if (percent >= 0) progress.setValue(Math.round(percent));
            });
        }
    }
}

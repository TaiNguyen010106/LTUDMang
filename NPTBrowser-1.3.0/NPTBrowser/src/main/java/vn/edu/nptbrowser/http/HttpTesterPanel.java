package vn.edu.nptbrowser.http;

import vn.edu.nptbrowser.config.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

public final class HttpTesterPanel extends JPanel {
    private static final Color SUCCESS = new Color(20, 120, 70);
    private static final Color ERROR = new Color(180, 45, 45);

    private final AppConfig config;
    private final JComboBox<String> methodBox = new JComboBox<>(new String[]{"GET", "POST", "PUT", "PATCH", "DELETE"});
    private final JTextField urlField = new JTextField();
    private final JTextArea requestHeaders = textArea();
    private final JTextArea requestBody = textArea();
    private final JTextArea responseHeaders = textArea();
    private final JTextArea responseBody = textArea();
    private final JLabel statusLabel = new JLabel("Chưa gửi request");
    private final JLabel timeLabel = new JLabel("-");
    private final JButton sendButton = new JButton("Gửi request");
    private final HttpClientService service;

    public HttpTesterPanel(AppConfig config) {
        super(new BorderLayout(8, 8));
        this.config = config;
        this.service = new HttpClientService(Duration.ofSeconds(config.timeoutSeconds()));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUi();
    }

    private void buildUi() {
        JPanel requestLine = new JPanel(new BorderLayout(8, 0));
        methodBox.setPrototypeDisplayValue("DELETE");
        requestLine.add(methodBox, BorderLayout.WEST);
        urlField.setText(config.apiBaseUrl() + "/api/time");
        requestLine.add(urlField, BorderLayout.CENTER);
        sendButton.addActionListener(event -> sendRequest());
        requestLine.add(sendButton, BorderLayout.EAST);
        add(requestLine, BorderLayout.NORTH);

        requestHeaders.setText("Accept: application/json\nContent-Type: application/json");
        responseHeaders.setEditable(false);
        responseBody.setEditable(false);

        JTabbedPane requestTabs = new JTabbedPane();
        requestTabs.addTab("Header yêu cầu", new JScrollPane(requestHeaders));
        requestTabs.addTab("Nội dung yêu cầu", new JScrollPane(requestBody));

        JTabbedPane responseTabs = new JTabbedPane();
        responseTabs.addTab("Header phản hồi", new JScrollPane(responseHeaders));
        responseTabs.addTab("Nội dung phản hồi", new JScrollPane(responseBody));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, requestTabs, responseTabs);
        split.setResizeWeight(0.42);
        split.setDividerLocation(250);
        add(split, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        metrics.add(new JLabel("Trạng thái:"));
        metrics.add(statusLabel);
        metrics.add(new JLabel("Thời gian:"));
        metrics.add(timeLabel);
        footer.add(metrics, BorderLayout.WEST);

        JButton clearButton = new JButton("Xóa kết quả");
        clearButton.addActionListener(event -> clearResult());
        footer.add(clearButton, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private void sendRequest() {
        try {
            String method = (String) methodBox.getSelectedItem();
            URI uri = HttpClientService.parseHttpUri(urlField.getText());
            Map<String, List<String>> headers = HttpClientService.parseHeaders(requestHeaders.getText());
            HttpRequestData request = new HttpRequestData(
                    method,
                    uri,
                    headers,
                    requestBody.getText(),
                    Duration.ofSeconds(config.timeoutSeconds()));

            sendButton.setEnabled(false);
            statusLabel.setForeground(Color.DARK_GRAY);
            statusLabel.setText("Đang gửi " + method + "...");
            timeLabel.setText("-");
            responseHeaders.setText("");
            responseBody.setText("");

            service.send(request).whenComplete((response, error) -> SwingUtilities.invokeLater(() -> {
                sendButton.setEnabled(true);
                if (error != null) {
                    showError(error);
                } else {
                    showResponse(response);
                }
            }));
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Request không hợp lệ", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showResponse(HttpResponseData response) {
        statusLabel.setText(response.statusCode() + " " + reasonPhrase(response.statusCode()));
        statusLabel.setForeground(response.statusCode() >= 200 && response.statusCode() < 400 ? SUCCESS : ERROR);
        timeLabel.setText(response.elapsed().toMillis() + " ms");

        StringBuilder headersText = new StringBuilder();
        response.headers().forEach((name, values) ->
                values.forEach(value -> headersText.append(name).append(": ").append(value).append('\n')));
        responseHeaders.setText(headersText.toString());
        responseHeaders.setCaretPosition(0);
        responseBody.setText(response.body());
        responseBody.setCaretPosition(0);
    }

    private void showError(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        statusLabel.setText("Lỗi kết nối");
        statusLabel.setForeground(ERROR);
        responseBody.setText(cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

    private void clearResult() {
        responseHeaders.setText("");
        responseBody.setText("");
        statusLabel.setText("Chưa gửi request");
        statusLabel.setForeground(Color.DARK_GRAY);
        timeLabel.setText("-");
    }

    public void refreshApiBaseUrl() {
        urlField.setText(config.apiBaseUrl() + "/api/time");
    }

    private static JTextArea textArea() {
        JTextArea area = new JTextArea();
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setLineWrap(false);
        area.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        return area;
    }

    private static String reasonPhrase(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "";
        };
    }
}

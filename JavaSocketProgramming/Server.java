import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP server phục vụ đồng thời nhiều Client và hỗ trợ broadcast tin nhắn.
 */
public class Server {
    private static final int PORT = 5000;
    private static final String EXIT_COMMAND = "exit";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final AtomicInteger nextClientId = new AtomicInteger(1);
    private static final AtomicInteger connectedClientCount = new AtomicInteger();
    private static final Map<Integer, ClientConnection> clients = new ConcurrentHashMap<>();
    private static final ExecutorService clientTasks = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("Server đang chạy tại port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Đang chờ Client kết nối...");
            startServerConsole();

            while (true) {
                Socket socket = serverSocket.accept();
                int clientId = nextClientId.getAndIncrement();
                ClientConnection client;
                try {
                    client = new ClientConnection(clientId, socket);
                } catch (IOException exception) {
                    System.err.println("Không thể khởi tạo kết nối Client #"
                            + clientId + ": " + exception.getMessage());
                    try {
                        socket.close();
                    } catch (IOException closeException) {
                        System.err.println("Không thể đóng socket bị lỗi: "
                                + closeException.getMessage());
                    }
                    continue;
                }
                clients.put(clientId, client);
                int currentCount = connectedClientCount.incrementAndGet();

                System.out.printf("Client #%d đã kết nối lúc %s (%s)%n",
                        clientId, formatTime(client.connectedAt), socket.getInetAddress());
                System.out.println("Clients đang kết nối: " + currentCount);
                client.send("Bạn là Client #" + clientId);

                clientTasks.submit(() -> handleClient(client));
            }
        } catch (IOException exception) {
            System.err.println("Lỗi Server: " + exception.getMessage());
        } finally {
            clientTasks.shutdownNow();
            clients.values().forEach(ClientConnection::close);
        }
    }

    private static void startServerConsole() {
        Thread consoleTask = new Thread(() -> {
            try (BufferedReader consoleReader = new BufferedReader(
                    new InputStreamReader(System.in))) {
                String message;
                while ((message = consoleReader.readLine()) != null) {
                    if (!message.trim().isEmpty()) {
                        broadcastToAll("Server: " + message);
                    }
                }
            } catch (IOException exception) {
                System.err.println("Lỗi đọc tin nhắn từ Server: "
                        + exception.getMessage());
            }
        }, "server-console");
        consoleTask.setDaemon(true);
        consoleTask.start();
    }

    private static void handleClient(ClientConnection client) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.socket.getInputStream()))) {
            String message;
            while ((message = reader.readLine()) != null) {


                if (EXIT_COMMAND.equalsIgnoreCase(message.trim())) {
                    client.send("Server đã nhận lệnh kết thúc. Tạm biệt!");
                    break;
                }

                client.send("UPPERCASE: " + message.toUpperCase(Locale.ROOT));
                broadcast(client.id, "Client #" + client.id + ": " + message);
            }
        } catch (IOException exception) {
            System.out.printf("Client #%d mất kết nối: %s%n", client.id, exception.getMessage());
        } finally {
            disconnect(client);
        }
    }

    private static void broadcast(int senderId, String message) {
        for (ClientConnection client : clients.values()) {
            if (client.id != senderId) {
                client.send(message);
            }
        }
    }

    private static void broadcastToAll(String message) {
        for (ClientConnection client : clients.values()) {
            client.send(message);
        }
    }

    private static void disconnect(ClientConnection client) {
        if (clients.remove(client.id, client)) {
            client.close();
            int currentCount = connectedClientCount.decrementAndGet();
            LocalDateTime disconnectedAt = LocalDateTime.now();
            Duration duration = Duration.between(client.connectedAt, disconnectedAt);
            System.out.printf("Client #%d đã ngắt kết nối lúc %s (đã kết nối %d giây)%n",
                    client.id, formatTime(disconnectedAt), duration.getSeconds());
            System.out.println("Clients đang kết nối: " + currentCount);
        } else {
            client.close();
        }
    }

    private static String formatTime(LocalDateTime time) {
        return time.format(TIME_FORMAT);
    }

    private static final class ClientConnection {
        private final int id;
        private final Socket socket;
        private final PrintWriter writer;
        private final LocalDateTime connectedAt = LocalDateTime.now();

        private ClientConnection(int id, Socket socket) throws IOException {
            this.id = id;
            this.socket = socket;
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        }

        private synchronized void send(String message) {
            if (!socket.isClosed()) {
                writer.println(message);
            }
        }

        private void close() {
            try {
                socket.close();
            } catch (IOException exception) {
                System.err.printf("Không thể đóng socket Client #%d: %s%n",
                        id, exception.getMessage());
            }
        }
    }
}

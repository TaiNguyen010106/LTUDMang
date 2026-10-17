import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Client TCP gửi tin nhắn và nhận phản hồi/broadcast từ Server.
 */
public class Client {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5000;
    private static final String EXIT_COMMAND = "exit";

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             BufferedReader serverReader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader keyboardReader = new BufferedReader(
                     new InputStreamReader(System.in))) {

            System.out.println("Đã kết nối Server " + SERVER_HOST + ":" + SERVER_PORT);
            Thread responseReader = new Thread(() -> readServerMessages(serverReader),
                    "server-response-reader");
            responseReader.setDaemon(true);
            responseReader.start();

            System.out.println("Nhập tin nhắn (gõ '" + EXIT_COMMAND + "' để kết thúc):");
            String message;
            while ((message = keyboardReader.readLine()) != null) {
                serverWriter.println(message);
                if (EXIT_COMMAND.equalsIgnoreCase(message.trim())) {
                    break;
                }
                System.out.print("Nhập tin nhắn: ");
            }
        } catch (IOException exception) {
            System.err.println("Không thể kết nối hoặc trao đổi dữ liệu với Server: "
                    + exception.getMessage());
        }

        System.out.println("Client đã kết thúc.");
    }

    private static void readServerMessages(BufferedReader serverReader) {
        try {
            String response;
            while ((response = serverReader.readLine()) != null) {
                System.out.println("\nServer: " + response);
                System.out.print("Nhập tin nhắn: ");
            }
        } catch (IOException exception) {
            System.out.println("\nKết nối đến Server đã đóng.");
        }
    }
}

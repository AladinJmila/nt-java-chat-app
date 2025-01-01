import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static String host = "localhost";
    private static int port = 8888;

    public static void main(String[] args) {
        try (Socket socket = new Socket(host, port)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println("Server message: " + serverMessage);
                    }
                } catch (IOException e) {
                    System.err.println(e);
                }
            }).start();

            String clientMessage;
            while ((clientMessage = consoleReader.readLine()) != null) {
                out.println(clientMessage);
            }

        } catch (IOException e) {
            System.err.println(e);
        }
    }
}

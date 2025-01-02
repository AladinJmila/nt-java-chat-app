import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private String host = "localhost";
    private int port = 8888;

    public void init() {
        try (Socket socket = new Socket(host, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);) {

            listenToIncomingMessages(in);
            handleClientInput(consoleReader, out);

        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void listenToIncomingMessages(BufferedReader in) {
        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }).start();
    }

    private void handleClientInput(BufferedReader in, PrintWriter out) throws IOException {
        String clientMessage;
        while ((clientMessage = in.readLine()) != null) {
            out.println(clientMessage);
        }
    }

    public static void main(String[] args) {
        new Client().init();
    }
}

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int PORT = 8888;

    public static void main(String[] args) {
        try (ExecutorService pool = Executors.newFixedThreadPool(50)) {
            try (ServerSocket server = new ServerSocket(PORT)) {
                System.out.println("Listening for connections on port " + PORT + "...");
                while (true) {
                    Socket connection = server.accept();
                    Runnable task = new ClientHandler(connection);
                    pool.execute(task);
                }
            } catch (UnknownHostException e) {
                System.out.println("Can't connect to host");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println(e);
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket client;

        ClientHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                var out = new PrintWriter(client.getOutputStream(), true);
                var in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                out.println("Hello from server");

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}

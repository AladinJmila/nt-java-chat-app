import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public final int PORT = 8888;
    private boolean running = true;
    private ServerSocket serverSocket;

    private void init() {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            try (ServerSocket server = new ServerSocket(PORT)) {
                serverSocket = server;
                System.out.println("Listening for connections on port " + PORT + "...");
                while (running) {
                    Socket clientSocket = server.accept();
                    Runnable task = new ClientHandler(clientSocket);
                    pool.execute(task);
                }
            } catch (UnknownHostException e) {
                System.out.println("Can't connect to host");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                if (!serverSocket.isClosed())
                    serverSocket.close();
                running = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        SoundNotification notification = new SoundNotification();
        private Socket clientSocket;

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);) {

                out.println("Hello from server");

                String clientMessage;
                while ((!clientSocket.isClosed()) && ((clientMessage = in.readLine()) != null)) {
                    if (clientMessage.startsWith("/q")) {
                        out.println("You're being diconnected...");
                        clientSocket.close();
                        break;
                    }
                    System.out.println(clientMessage);
                    notification.play();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws Exception {
        new Server().init();
    }

}

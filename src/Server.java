import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public final int PORT = 8888;
    private boolean running = true;
    private ServerSocket serverSocket;
    private CopyOnWriteArrayList<ClientHandler> handlers = new CopyOnWriteArrayList<>();

    private void init() {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            try (ServerSocket server = new ServerSocket(PORT)) {
                serverSocket = server;
                System.out.println("Listening for connections on port " + PORT + "...");
                while (running) {
                    Socket clientSocket = server.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    handlers.add(handler);
                    pool.execute(handler);
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
        private String clientName = "Guest " + (handlers.size() + 1);

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);) {
                out.println("Hi " + clientName);
                out.println("Welcome to the Main chatroom!");

                String clientMessage;
                while ((!clientSocket.isClosed()) && ((clientMessage = in.readLine()) != null)) {
                    if (clientMessage.startsWith("/q")) {
                        out.println("You're being diconnected...");
                        clientSocket.close();
                        break;
                    }
                    System.out.println(clientName + ": " + clientMessage);
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

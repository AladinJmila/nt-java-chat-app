import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 7777;
    private int port;
    private boolean running = true;
    private ServerSocket serverSocket;
    private CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public Server() {
        this.port = PORT;
    }

    public Server(int port) {
        this.port = (port != 0) ? port : PORT;
    }

    private void init() {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            try (ServerSocket server = new ServerSocket(port)) {
                serverSocket = server;
                System.out.println("Listening for connections on port " + port + "...");
                while (running) {
                    Socket clientSocket = server.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
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

    private void broadcast(ClientHandler currentClient, String message) {
        for (var client : clients) {
            if (client != currentClient && message != null)
                client.sendMessage(message);
        }
    }

    private class ClientHandler implements Runnable {
        SoundNotification notification = new SoundNotification();
        PrintWriter writer;
        private Socket clientSocket;
        private String clientName = "Guest " + (clients.size() + 1);

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);) {
                writer = out;
                out.println("Hi " + clientName);
                out.println("Welcome to the Main chatroom!");

                String clientMessage;
                while ((!clientSocket.isClosed()) && ((clientMessage = in.readLine()) != null)) {
                    if (clientMessage.startsWith("/q")) {
                        out.println("You're being diconnected...");
                        broadcast(this, clientName + " left the chat.");
                        clientSocket.close();
                        break;
                    }
                    broadcast(this, clientName + ": " + clientMessage);
                    notification.play();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            writer.println(message);
        }

    }

    public static void main(String[] args) {
        Properties config = new Properties();
        try (FileInputStream configFileStream = new FileInputStream("./config.properties")) {
            config.load(configFileStream);
            int port = Integer.parseInt(config.getProperty("PORT"));
            new Server(port).init();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("Proceeding with default PORT:");
            new Server().init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

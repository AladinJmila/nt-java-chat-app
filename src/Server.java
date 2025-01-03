import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Multi-threaded chat server that manages client connections and chat rooms
public class Server {
    private static final int PORT = 7777;
    private int port;
    private boolean running = true;
    private ServerSocket serverSocket;
    // Thread-safe list to store all connected client handlers
    private CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    // Predefined chat rooms available in the server
    private Map<Integer, String> chatRooms = Map.of(0, "Main", 1, "Movies", 2, "Sports", 3, "Crafts");

    // Default constructor using predefined port
    public Server() {
        this.port = PORT;
    }

    // Constructor allowing custom port configuration with fallback to default
    public Server(int port) {
        this.port = (port != 0) ? port : PORT;
    }

    // Initializes the server and starts accepting client connections
    private void init() {
        monitorConnections();
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

    // Broadcasts a message to all connected clients except the sender
    private void broadcastToAll(ClientHandler currentClient, String message) {
        for (var client : clients) {
            if (client != currentClient && message != null)
                client.sendMessage(message);
        }
    }

    // Broadcasts a message only to clients in the same chat room as the sender
    private void broadcastToRoom(ClientHandler currentClient, String message) {
        for (var client : clients) {
            if (client != currentClient && message != null && client.chatRoomId == currentClient.chatRoomId)
                client.sendMessage(message);
        }
    }

    // Periodically checks for and removes disconnected clients
    private void monitorConnections() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            for (var client : clients) {
                if (client.getSocket().isClosed()) {
                    broadcastToAll(client, client.getName() + " left the chat.");
                    broadcastToAll(client, "Currently there are " + (clients.size() - 1) + " user(s) on the platform.");
                    clients.remove(client);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    // Handles individual client connections and message processing
    private class ClientHandler implements Runnable {
        SoundNotification notification = new SoundNotification();
        PrintWriter writer;
        private Socket clientSocket;
        private String clientName = "Guest " + (clients.size() + 1);
        private int chatRoomId = 0;

        // Initializes a new client handler with the client's socket
        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        // Manages client connection lifecycle and message handling
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);) {
                writer = out;

                renderWelcomeMenu();
                broadcastToRoom(this, clientName + " joined the chat.");

                String clientMessage;
                while ((!clientSocket.isClosed()) && ((clientMessage = in.readLine()) != null)) {
                    clientMessage = clientMessage.trim();
                    if (clientMessage.startsWith("/q")) {
                        handleQuit(this);
                        break;
                    } else if (clientMessage.startsWith("/r")) {
                        handleRoomChange(clientMessage);
                    } else {
                        broadcastToRoom(this, clientName + ": " + clientMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Sends a message to the client and plays a notification sound
        public void sendMessage(String message) {
            writer.println(message);
            notification.play();
        }

        // Displays initial welcome message and available commands to the client
        private void renderWelcomeMenu() {
            writer.println("Hi " + clientName);
            writer.println("Welcome to the '" + chatRooms.get(chatRoomId) + "' chatroom!");
            writer.println("Currently there are " + clients.size() + " user(s) on the platform including you.");
            writer.println();
            writer.println("To change the chatroom, enter /r followed by:");
            writer.println("   - 0 for '" + chatRooms.get(0) + "'");
            writer.println("   - 1 for '" + chatRooms.get(1) + "'");
            writer.println("   - 2 for '" + chatRooms.get(2) + "'");
            writer.println("   - 3 for '" + chatRooms.get(3) + "'");
            writer.println("To exit the chat, enter /q");
            writer.println();
        }

        // Handles client disconnection and cleanup
        private void handleQuit(ClientHandler currentClient) throws IOException {
            writer.println("You're being diconnected...");
            broadcastToAll(this, clientName + " left the chat.");
            broadcastToAll(this, "Currently there are " + clients.size() + " user(s) on the platform.");
            clientSocket.close();
            clients.remove(currentClient);
        }

        // Processes chat room change requests from clients
        private void handleRoomChange(String clientMessage) {
            try {
                String input = clientMessage.split(" ")[1];
                if (!input.matches("\\d+")) {
                    writer.println("Please enter /r followed by a valid room number (numbers only)");
                    return;
                }

                int roomNumber = Integer.parseInt(input);

                if (roomNumber == 0 || roomNumber == 1 || roomNumber == 2 || roomNumber == 3) {
                    chatRoomId = roomNumber;
                    writer.println("You successfully changed rooms. Welcome to '" + chatRooms.get(chatRoomId)
                            + "'");
                    broadcastToAll(this,
                            clientName + " joined '" + chatRooms.get(chatRoomId) + "' chatroom");
                } else {
                    writer.println("Incorrect option. You are still in '" + chatRooms.get(chatRoomId) + "'");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Socket getSocket() {
            return this.clientSocket;
        }

        public String getName() {
            return this.clientName;
        }
    }

    // Entry point that loads configuration and starts the server
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

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

public class Server {
    private static final int PORT = 7777;
    private int port;
    private boolean running = true;
    private ServerSocket serverSocket;
    private CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private Map<Integer, String> chatRooms = Map.of(0, "Main", 1, "Movies", 2, "Sports", 3, "Crafts");

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

    private void broadcastToAll(ClientHandler currentClient, String message) {
        for (var client : clients) {
            if (client != currentClient && message != null)
                client.sendMessage(message);
        }
    }

    private void broadcastToRoom(ClientHandler currentClient, String message) {
        for (var client : clients) {
            if (client != currentClient && message != null && client.chatRoomId == currentClient.chatRoomId)
                client.sendMessage(message);
        }
    }

    private class ClientHandler implements Runnable {
        SoundNotification notification = new SoundNotification();
        PrintWriter writer;
        private Socket clientSocket;
        private String clientName = "Guest " + (clients.size() + 1);
        private int chatRoomId = 0;

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

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
                        handleQuit();
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

        public void sendMessage(String message) {
            writer.println(message);
            notification.play();
        }

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

        private void handleQuit() throws IOException {
            writer.println("You're being diconnected...");
            broadcastToRoom(this, clientName + " left the chat.");
            clientSocket.close();
        }

        private void handleRoomChange(String clientMessage) {
            try {
                int roomNumber = Integer.parseInt(clientMessage.split(" ")[1]);
                if (roomNumber == 0 || roomNumber == 1 || roomNumber == 2 || roomNumber == 3) {
                    chatRoomId = roomNumber;
                    writer.println("You successfully changed rooms. Welcome to '" + chatRooms.get(chatRoomId)
                            + "'");
                    broadcastToAll(this,
                            clientName + " joined '" + chatRooms.get(chatRoomId) + "' chatroom");
                } else {
                    writer.println("Incorrect option. You are still in '" + chatRooms.get(chatRoomId) + "'");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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

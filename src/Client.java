import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// A TCP client implementation that enables real-time chat communication with a server
public class Client {
    private static final int PORT = 7777;
    private static final String HOST = "localhost";
    private int port;
    private String host;

    // Default constructor that initializes client with default host and port
    public Client() {
        this.port = PORT;
        this.host = HOST;
    }

    // Constructor that allows custom host and port configuration with fallback to
    // defaults
    public Client(int port, String host) {
        this.port = (port != 0) ? port : PORT;
        this.host = (host != null) ? host : HOST;
    }

    // Initializes the client connection and sets up message handling
    public void init() {
        try (Socket socket = new Socket(host, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);) {

            System.out.println("Connected to HOST: '" + socket.getInetAddress() + "' on PORT: " + socket.getPort());

            monitorConnection(socket);
            listenToIncomingMessages(in);
            handleClientInput(consoleReader, out);

        } catch (ConnectException e) {
            System.err.println("Connection failed: Unable to connect to " + host + ":" + port);
            System.err.println("Please ensure the server is running ont PORT: " + port + " and try again.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Monitors socket connection health by sending periodic heartbeat signals
    private void monitorConnection(Socket socket) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                socket.getOutputStream().write(0);
            } catch (SocketException e) {
                System.err.println("Lost connection! Please try again later.");
                try {
                    socket.close();
                } catch (IOException err) {
                    err.printStackTrace();
                }
                System.err.println("Exiting application...");
                System.exit(1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    // Spawns a separate thread to continuously listen for incoming server messages
    private void listenToIncomingMessages(BufferedReader in) {
        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
            }
        }).start();
    }

    // Reads user input from console and sends it to the server
    private void handleClientInput(BufferedReader in, PrintWriter out) throws IOException {
        String clientMessage;
        while ((clientMessage = in.readLine()) != null) {
            out.println(clientMessage);
        }
    }

    // Entry point that loads configuration and starts the client
    public static void main(String[] args) {
        Properties config = new Properties();
        try (FileInputStream configFileStream = new FileInputStream("./config.properties")) {
            config.load(configFileStream);
            int port = Integer.parseInt(config.getProperty("PORT"));
            String host = config.getProperty("HOST");
            new Client(port, host).init();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("Proceeding with default PORT & HOST:");
            new Client().init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

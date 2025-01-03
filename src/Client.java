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

public class Client {
    private static final int PORT = 7777;
    private static final String HOST = "localhost";
    private int port;
    private String host;

    public Client() {
        this.port = PORT;
        this.host = HOST;
    }

    public Client(int port, String host) {
        this.port = (port != 0) ? port : PORT;
        this.host = (host != null) ? host : HOST;
    }

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

    private void monitorConnection(Socket socket) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                socket.getOutputStream().write(0);
            } catch (SocketException e) {
                System.err.println("Lost server connection! Please try again later.");
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

    private void listenToIncomingMessages(BufferedReader in) {
        Thread.startVirtualThread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
                System.err.println("Connection lost...");
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

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
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(message);
            }

            try (var input = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    out.println(input.readLine());
                }
            } catch (IOException e) {
                // TODO: handle exception
            }

        } catch (IOException e) {
            // TODO: handle exception
        }
    }
}

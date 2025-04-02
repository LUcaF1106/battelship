package  com.itis._5a.frasson.busanello;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;


public class Server {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        Auth auth = new Auth();
        Server server = new Server(PORT, auth);
        server.start();
    }

    private final int port;
    private final Auth auth;

    public Server(int port, Auth auth) {
        this.port = port;
        this.auth = auth;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server in ascolto sulla porta " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso: " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket, auth).start();
            }
        } catch (IOException e) {
            System.err.println("Errore nel server: " + e.getMessage());
        }
    }
}


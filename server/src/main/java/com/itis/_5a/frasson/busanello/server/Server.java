package com.itis._5a.frasson.busanello.server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;


public class Server {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        Auth auth = new Auth();
        Server server = new Server(PORT, auth);
        server.start();
    }

    private final int port;
    private final Auth auth;
    private ArrayList<ClientHandler> clientList;


    public Server(int port, Auth auth) {
        this.port = port;
        this.auth = auth;
        this.clientList = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server in ascolto sulla porta " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso: " + clientSocket.getInetAddress());

                ClientHandler c=new ClientHandler(clientSocket, auth);
                clientList.add(c);
                Thread t=new Thread(c);
                t.start();
                //TODO implementare stato se sono alla richerca di anlti in game o sono sulla schermata main o in gioco
            }
        } catch (IOException e) {
            System.err.println("Errore nel server: " + e.getMessage());
        }
    }
}


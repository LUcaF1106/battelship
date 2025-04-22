package com.itis._5a.frasson.busanello.server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Server {
    private static final int PORT = 12345;
    private static Server instance;
    private static final Auth authentication = new Auth();

    public static synchronized Server getInstance() {
        if (instance == null) {
            instance = new Server(PORT, authentication);
        }
        return instance;
    }

    public static void main(String[] args) {
        Server server = getInstance();
        server.start();
    }

    private final int port;
    private final Auth auth;
    private ArrayList<ClientHandler> clientList;
    private final BlockingQueue<ClientHandler> queue;

    public Server(int port, Auth auth) {
        this.port = port;
        this.auth = auth;
        this.clientList = new ArrayList<>();
        this.queue = new LinkedBlockingQueue<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server in ascolto sulla porta " + port);
            Thread tMatch=new Thread(this::matchMaking);
                    tMatch.start();


            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso: " + clientSocket.getInetAddress());

                ClientHandler c=new ClientHandler(clientSocket, auth);
                clientList.add(c);
                Thread t=new Thread(c);
                t.start();

            }
        } catch (IOException e) {
            System.err.println("Errore nel server: " + e.getMessage());
        }
    }

    public void matchMaking() {
        try {
            while (true) {
                ClientHandler client1 = queue.take();
                ClientHandler client2 = queue.take();

                Match match= new Match(client1, client2);

                client1.setCurrentMatch(match);
                client2.setCurrentMatch(match);

                Thread t=new Thread(match);
                t.start();

            }
        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
            System.out.println("Errore durante il matchmaking.");
        }
    }
    public void enqueue(ClientHandler client) {
        try {
            queue.put(client);

        } catch (InterruptedException e) {

            System.out.println("Errore durante enqueue.");
        }
    }
}


package com.itis._5a.frasson.busanello.server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;


public class Server {
    private static final Logger LOGGER = LogManager.getLogger(Server.class);
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
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);
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
            LOGGER.info("Server listening on port " + port);
            Thread tMatch=new Thread(this::matchMaking);
                    tMatch.start();


            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("New client connected: " + clientSocket.getInetAddress());

                ClientHandler c=new ClientHandler(clientSocket, auth);
                clientList.add(c);
                Thread t=new Thread(c);
                t.start();

            }
        } catch (IOException e) {
            LOGGER.error("Server error", e);
        }
    }

    public void matchMaking() {
        LOGGER.info("Matchmaking service started");
        try {
            while (true) {
                ClientHandler client1 = queue.take();
                LOGGER.info("Player 1 ready for matchmaking: " + client1.getId());
                ClientHandler client2 = queue.take();
                LOGGER.info("Player 2 ready for matchmaking: " + client2.getId());
                Match match= new Match(client1, client2);

                client1.setCurrentMatch(match);
                client2.setCurrentMatch(match);

                Thread t=new Thread(match);
                t.start();
                LOGGER.info("Match created and started between players: " + client1.getId() + " and " + client2.getId());

            }
        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
            LOGGER.error("Matchmaking error", e);

        }
    }
    public void enqueue(ClientHandler client) {
        try {
            queue.put(client);
            LOGGER.info("Player enqueued for matchmaking: " + client.getId());
        } catch (InterruptedException e) {
            LOGGER.error("Error during enqueue", e);
        }
    }
}


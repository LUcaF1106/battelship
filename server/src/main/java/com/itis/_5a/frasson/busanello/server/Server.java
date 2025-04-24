package com.itis._5a.frasson.busanello.server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;


public class Server {
    private static final Logger LOGGER = LogManager.getLogger(Server.class);
    private static final int PORT = 12345;
    private static Server instance;
    private static final Auth authentication = new Auth();

    private final ExecutorService clientThreadPool;
    private final ExecutorService matchThreadPool;

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
    private final BlockingQueue<ClientHandler> matchQueue;

    public Server(int port, Auth auth) {
        this.port = port;
        this.auth = auth;
        this.clientList = new ArrayList<>();
        this.matchQueue = new LinkedBlockingQueue<>();

        this.clientThreadPool = Executors.newCachedThreadPool();
        this.matchThreadPool = Executors.newFixedThreadPool(10);

    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("Server listening on port " + port);
            clientThreadPool.submit(this::matchMaking);


            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("New client connected: " + clientSocket.getInetAddress());

                ClientHandler c=new ClientHandler(clientSocket, auth);
                clientList.add(c);
                clientThreadPool.submit(c);

            }
        } catch (IOException e) {
            LOGGER.error("Server error", e);
        }
    }

    public void matchMaking() {
        LOGGER.info("Matchmaking service started");
        try {
            while (true) {
                ClientHandler client1 = matchQueue.take();
                if (isClientDisconnected(client1)) {
                    LOGGER.info("Client 1 disconnected while in queue: " + client1.getId());
                    continue;
                }
                client1.setState(1);
                LOGGER.info("Player 1 ready for match: " + client1.getId());

                ClientHandler client2 = null;
                while (client2 == null) {
                    try {
                        client2 = matchQueue.poll(5, TimeUnit.SECONDS);
                        if (client2 == null) {

                            if (isClientDisconnected(client1)) {
                                LOGGER.info("Client 1 disconnected while waiting for opponent: " + client1.getId());
                                break;
                            }
                            continue;
                        }
                        if (isClientDisconnected(client2)) {
                            LOGGER.info("Client 2 disconnected while in queue: " + client2.getId());
                            client2 = null;
                            continue;
                        }
                    } catch (InterruptedException e) {
                        LOGGER.error("Matchmaking interrupted while waiting for second player", e);
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                if (client2 == null || isClientDisconnected(client1)) {
                    continue;
                }

                client2.setState(1);
                LOGGER.info("Player 2 ready for matchmaking: " + client2.getId());

                Match match = new Match(client1, client2);
                client1.setCurrentMatch(match);
                client2.setCurrentMatch(match);

                matchThreadPool.submit(match);
                LOGGER.info("Match created and started between players: " + client1.getId() + " and " + client2.getId());

            }
        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
            LOGGER.error("Matchmaking error", e);

        }
    }
    private boolean isClientDisconnected(ClientHandler client) {
        return client == null ||
                !clientList.contains(client) ||
                client.getClientSocket().isClosed();
    }

    public void clientDisconnected(ClientHandler client) {
        if (client != null) {
            LOGGER.info("Handling disconnect for client: " + client.getId());

            clientList.remove(client);

            if (client.getCurrentMatch() != null) {
                client.getCurrentMatch().handleDisconnect(client);
            }
        }
    }

    public void enqueue(ClientHandler client) {
        try {

            if (!isClientDisconnected(client)) {
                matchQueue.put(client);
                LOGGER.info("Player enqueued for matchmaking: " + client.getId());
            } else {
                LOGGER.warn("Attempted to enqueue disconnected client: " + client.getId());
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error during enqueue", e);
        }
    }

    public void removeFromQueue(ClientHandler client) {
        matchQueue.remove(client);
        LOGGER.info("Player removed from matchmaking queue: " + client.getId());
    }



}


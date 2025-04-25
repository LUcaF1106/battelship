package com.itis._5a.frasson.busanello.server;

import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.Message;
import com.itis._5a.frasson.busanello.common.Message.Move;
import com.itis._5a.frasson.busanello.common.Message.ResultMove;
import com.itis._5a.frasson.busanello.common.Message.Turn;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class Match implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(Match.class);

    private boolean sendTurn = false;
    private ClientHandler client1;
    private ClientHandler client2;


    private int[][] mapShip1 = new int[10][10];
    private int[][] moveMap1 = new int[10][10];


    private int[][] mapShip2 = new int[10][10];
    private int[][] moveMap2 = new int[10][10];

    private boolean p1Ready = false;
    private boolean p2Ready = false;
    private boolean c1turn = true;


    private int p1ShipsRemaining = 5;
    private int p2ShipsRemaining = 5;

    public Match(ClientHandler c1, ClientHandler c2) {
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);

        this.client1 = c1;
        this.client2 = c2;
        LOGGER.info("Match created between players: {} and {}", c1.getId(), c2.getId());
    }

    @Override
    public void run() {


        //Al avvio del thread imosta lo stato a 2 che significa che sono entrati su un match ee indica al client che è stato trovato un match
        client1.setState(2);
        client2.setState(2);

        try {
            client1.sendMessage(new Message("MFIND"));
            client2.sendMessage(new Message("MFIND"));
            LOGGER.info("Match initialized, sending MFIND to both players");
        } catch (Exception e) {
            LOGGER.error("Error initializing match", e);

            throw new RuntimeException(e);
        }
    }

    public synchronized void playerReady(ClientHandler c) {
        if (c == client1) {
            p1Ready = true;
            LOGGER.info("Player 1 ({}) is ready", client1.getId());
        }
        else if (c == client2) {
            p2Ready = true;
            LOGGER.info("Player 2 ({}) is ready", client2.getId());
        }

        if (p1Ready && p2Ready) {
            LOGGER.info("Both players ready, starting game");
            try {
                startGame();
            } catch (Exception e) {
                LOGGER.error("Error starting game", e);

                throw new RuntimeException(e);
            }
        }
    }

    public void startGame() throws Exception {
        client1.sendMessage(new Message("PLAY"));
        client1.setState(3);
        client2.sendMessage(new Message("PLAY"));
        client2.setState(3);
        LOGGER.info("Game started between {} and {}", client1.getId(), client2.getId());
    }



    public void setMapShip(int[][] map, ClientHandler c) {
        if (c == client1) {
            mapShip1 = map;
            LOGGER.debug("Player 1 ({}) placed ships", client1.getId());
        } else if (c == client2) {
            mapShip2 = map;
            LOGGER.debug("Player 2 ({}) placed ships", client2.getId());
        }
        playerReady(c);
    }

    public synchronized void setInitialTurn(ClientHandler c) throws Exception {
        if (!sendTurn) {
            if (c == client1) {

                client1.sendMessage(new Turn("YT", mapShip1, moveMap1));

                client2.sendMessage(new Turn("OT", mapShip2, moveMap2));
                c1turn = true;
                LOGGER.debug("Initial turn set for Player 1 ({})", client1.getId());
            } else if (c == client2) {

                client2.sendMessage(new Turn("YT", mapShip2, moveMap2));

                client1.sendMessage(new Turn("OT", mapShip1, moveMap1));
                c1turn = false;
                LOGGER.debug("Initial turn set for Player 2 ({})", client2.getId());
            }
            sendTurn = true;
        }
    }



    public String checkMoves(int x, int y, int[][] map, int[][] moves) {

        if (x < 0 || x >= map.length || y < 0 || y >= map[0].length) {
            LOGGER.warn("Invalid coordinates: [{},{}]", x, y);
            return "ACQUA";
        }

        int shipType = map[x][y];

        if (shipType == 0) {

            moves[x][y] = 999;
            LOGGER.debug("Hit water at [{},{}]", x, y);
            return "ACQUA";
        } else {
            moves[x][y] = shipType;
            LOGGER.debug("Hit ship type {} at [{},{}]", shipType, x, y);

            boolean isSunk = checkIfSunk(shipType, map, moves);

            if (isSunk) {
                if (map == mapShip1) {
                    p1ShipsRemaining--;
                    LOGGER.info("Player 1 ship type {} SUNK! Remaining ships: {}", shipType, p1ShipsRemaining);
                } else {
                    p2ShipsRemaining--;
                    LOGGER.info("Player 2 ship type {} SUNK! Remaining ships: {}", shipType, p2ShipsRemaining);
                }
                return "AFFONDATO";
            } else {
                return "COLPITO";
            }
        }
    }

    private boolean checkIfSunk(int shipType, int[][] map, int[][] moves) {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                if (map[i][j] == shipType && moves[i][j] != shipType) {
                    return false;
                }
            }
        }
        LOGGER.debug("Ship type {} is completely sunk!", shipType);
        return true;
    }

    public void processMessage(ClientHandler client, Message message, byte[] messageDecrypted) {
        try {
            switch (message.getType()) {
                case "TURN":
                    LOGGER.debug("TURN request received from {}", (client == client1 ? "Player 1" : "Player 2"));

                    setInitialTurn(client);
                    break;
                case "MOVE":
                    LOGGER.debug("MOVE received from {}", (client == client1 ? "Player 1" : "Player 2"));

                    Move move = Json.deserializedSpecificMessage(messageDecrypted, Move.class);
                    LOGGER.debug("Move details: [" + move.getX() + "," + move.getY() + "]");
                    handleMove(client, move);
                    break;
                case "EXIT":
                    LOGGER.info("{} is disconnected from match", (client == client1 ? "Player 1" : "Player 2"));

                    break;
                default:
                    LOGGER.warn("Unhandled game message type: {}", message.getType());
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Error processing match message", e);

        }
    }

    private void handleMove(ClientHandler client, Move move) {
        synchronized(this) {
            String result;
            boolean gameOver = false;

            if (client == client1 && c1turn) {
                result = checkMoves(move.getX(), move.getY(), mapShip2, moveMap2);
                if (p2ShipsRemaining <= 0) {
                    gameOver = true;
                    result = "VITTORIA";
                }
                c1turn = false;
            } else if (client == client2 && !c1turn) {
                result = checkMoves(move.getX(), move.getY(), mapShip1, moveMap1);
                if (p1ShipsRemaining <= 0) {
                    gameOver = true;
                    result = "VITTORIA";
                }
                c1turn = true;
            } else {
                LOGGER.warn("Move received from wrong player");
                return;
            }

            try {
                sendMessageResult(move, result, gameOver, client);
            } catch (Exception e) {
                LOGGER.error("Error sending move result", e);

                e.printStackTrace();
            }
        }
    }

    private void sendMessageResult(Move move, String result, boolean gameOver, ClientHandler moveClient) throws Exception {
        ResultMove playerResult = new ResultMove(move.getX(), move.getY(), result, gameOver);

        String opponentResult = result;
        if (result.equals("VITTORIA")) {
            opponentResult = "SCONFITTA";
            LOGGER.info("Sending victory message to Player {}", (moveClient == client1 ? "Player 1" : "Player 2"));
            LOGGER.info("Sending defeat message to Player {}", (moveClient == client1 ? "Player 2" : "Player 1"));
        } else if (result.equals("SCONFITTA")) {
            opponentResult = "VITTORIA";
        } else {
            LOGGER.debug("Move result: {} at [{},{}]", result, move.getX(), move.getY());
        }

        ResultMove opponentMove = new ResultMove(move.getX(), move.getY(), opponentResult, gameOver);

        if (moveClient == client1) {
            client1.sendMessage(playerResult);
            client2.sendMessage(opponentMove);
        } else {
            client2.sendMessage(playerResult);
            client1.sendMessage(opponentMove);
        }

        if (gameOver) {
            LOGGER.info("======= GAME OVER =======");
            LOGGER.info("Winner: {}", (moveClient == client1 ? "Player 1" : "Player 2"));
            LOGGER.info("Player 1 ships remaining: {}", p1ShipsRemaining);
            LOGGER.info("Player 2 ships remaining: {}", p2ShipsRemaining);
            LOGGER.info("=========================");
        }
    }
    public void handleDisconnect(ClientHandler disconnectedPlayer) {

        LOGGER.info("Player disconnected from match: {}", disconnectedPlayer.getId());

        try {
            ClientHandler opponent = (disconnectedPlayer.equals(client1)) ? client2 : client1;

            if (opponent.isConnected()) {
                Message disconnectMessage = new Message("OPPONENT_DISCONNECTED");
                opponent.sendMessage(disconnectMessage);
                LOGGER.info("Sent disconnect notification to opponent: {}", opponent.getId());

            }

        } catch (Exception e) {
            LOGGER.error("Error handling disconnect in match", e);
        } finally {
            handleMatchEnd();
        }
    }

    private void handleMatchEnd() {

        LOGGER.info("Match ended between {} and {}", client1.getId(), client2.getId());

        client1.setCurrentMatch(null);
        client2.setCurrentMatch(null);

        if (client1.isConnected()) {
            client1.setState(0);
        }

        if (client2.isConnected()) {
            client2.setState(0);
        }
    }

}
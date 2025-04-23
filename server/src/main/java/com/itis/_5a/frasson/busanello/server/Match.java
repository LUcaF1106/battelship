package com.itis._5a.frasson.busanello.server;

import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.Message;
import com.itis._5a.frasson.busanello.common.Message.Move;
import com.itis._5a.frasson.busanello.common.Message.ResultMove;
import com.itis._5a.frasson.busanello.common.Message.Turn;

public class Match implements Runnable {
    private boolean sendTurn = false;
    private ClientHandler client1;
    private ClientHandler client2;

    // Player 1's board and moves made by player 2 on player 1's board
    private int[][] mapShip1 = new int[10][10];
    private int[][] moveMap1 = new int[10][10];

    // Player 2's board and moves made by player 1 on player 2's board
    private int[][] mapShip2 = new int[10][10];
    private int[][] moveMap2 = new int[10][10];

    private boolean p1Ready = false;
    private boolean p2Ready = false;
    private boolean c1turn = true;

    // Track remaining ships - start with 5 ships
    private int p1ShipsRemaining = 5;
    private int p2ShipsRemaining = 5;

    public Match(ClientHandler c1, ClientHandler c2) {
        this.client1 = c1;
        this.client2 = c2;
        System.out.println("Match created between Player 1 and Player 2");
    }

    public void setMapShip(int[][] map, ClientHandler c) {
        if (c == client1) {
            mapShip1 = map;
            System.out.println("Player 1 ships placed on the board");
        } else if (c == client2) {
            mapShip2 = map;
            System.out.println("Player 2 ships placed on the board");
        }
        playerReady(c);
    }

    public synchronized void setInitialTurn(ClientHandler c) throws Exception {
        if (!sendTurn) {
            if (c == client1) {
                // Client1's turn - send their ships and moves on client2's board
                client1.sendMessage(new Turn("YT", mapShip1, moveMap1));
                // Client2 waiting - send their ships and moves client1 made on their board
                client2.sendMessage(new Turn("OT", mapShip2, moveMap2));
                c1turn = true;
                System.out.println("Initial turn set to Player 1");
            } else if (c == client2) {
                // Client2's turn - send their ships and moves on client1's board
                client2.sendMessage(new Turn("YT", mapShip2, moveMap2));
                // Client1 waiting - send their ships and moves client2 made on their board
                client1.sendMessage(new Turn("OT", mapShip1, moveMap1));
                c1turn = false;
                System.out.println("Initial turn set to Player 2");
            }
            sendTurn = true;
        }
    }

    public synchronized void playerReady(ClientHandler c) {
        if (c == client1) {
            p1Ready = true;
            System.out.println("Player 1 is ready");
        }
        else if (c == client2) {
            p2Ready = true;
            System.out.println("Player 2 is ready");
        }

        if (p1Ready && p2Ready) {
            System.out.println("Both players ready, starting game...");
            try {
                startGame();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void startGame() throws Exception {
        client1.sendMessage(new Message("PLAY"));
        client1.setState(3);
        client2.sendMessage(new Message("PLAY"));
        client2.setState(3);
        System.out.println("Game started! Players in PLAY state.");
    }

    @Override
    public void run() {
        client1.setState(2);
        client2.setState(2);

        try {
            client1.sendMessage(new Message("MFIND"));
            client2.sendMessage(new Message("MFIND"));
            System.out.println("Match initialized, sending MFIND to both players");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String checkMoves(int x, int y, int[][] map, int[][] moves) {
        // Check boundaries
        if (x < 0 || x >= map.length || y < 0 || y >= map[0].length) {
            System.out.println("Invalid coordinates: [" + x + "," + y + "]");
            return "ACQUA";
        }

        int shipType = map[x][y];

        if (shipType == 0) {
            // Hit water
            moves[x][y] = 999; // Mark as water hit
            System.out.println("Hit water at [" + x + "," + y + "]");
            return "ACQUA";
        } else {
            // Hit a ship
            moves[x][y] = shipType; // Record the hit on the ship
            System.out.println("Hit ship type " + shipType + " at [" + x + "," + y + "]");

            // Check if ship is now sunk by comparing mapShip and moveMap
            boolean isSunk = checkIfSunk(shipType, map, moves);

            if (isSunk) {
                // A ship was just sunk, decrement ship counter
                if (map == mapShip1) {
                    p1ShipsRemaining--;
                    System.out.println("Player 1 ship type " + shipType + " SUNK! Remaining ships: " + p1ShipsRemaining);
                } else {
                    p2ShipsRemaining--;
                    System.out.println("Player 2 ship type " + shipType + " SUNK! Remaining ships: " + p2ShipsRemaining);
                }
                return "AFFONDATO";
            } else {
                return "COLPITO";
            }
        }
    }

    private boolean checkIfSunk(int shipType, int[][] map, int[][] moves) {
        // Check if all cells of this ship type are hit by comparing mapShip and moveMap
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                if (map[i][j] == shipType && moves[i][j] != shipType) {
                    // Found a part of the ship that hasn't been hit yet
                    return false;
                }
            }
        }
        // All parts of this ship have been hit
        System.out.println("Ship type " + shipType + " is completely sunk!");
        return true;
    }

    public void processMessage(ClientHandler client, Message message, byte[] messageDecrypted) {
        try {
            switch (message.getType()) {
                case "TURN":
                    System.out.println("TURN request received from " + (client == client1 ? "Player 1" : "Player 2"));
                    setInitialTurn(client);
                    break;
                case "MOVE":
                    System.out.println("MOVE received from " + (client == client1 ? "Player 1" : "Player 2"));
                    Move move = Json.deserializedSpecificMessage(messageDecrypted, Move.class);
                    System.out.println("Move details: [" + move.getX() + "," + move.getY() + "]");
                    handleMove(client, move);
                    break;

                default:
                    System.out.println("Unhandled game message type: " + message.getType());
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error processing match message: " + e.getMessage());
            e.printStackTrace();
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
                System.out.println("Move received from wrong player!");
                return;
            }

            try {
                sendMessageResult(move, result, gameOver, client);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessageResult(Move move, String result, boolean gameOver, ClientHandler moveClient) throws Exception {
        // Create result message for the player who made the move
        ResultMove playerResult = new ResultMove(move.getX(), move.getY(), result, gameOver);

        // Create result message for the opponent with appropriate result text
        String opponentResult = result;
        if (result.equals("VITTORIA")) {
            opponentResult = "SCONFITTA";
            System.out.println("Sending victory message to " + (moveClient == client1 ? "Player 1" : "Player 2"));
            System.out.println("Sending defeat message to " + (moveClient == client1 ? "Player 2" : "Player 1"));
        } else if (result.equals("SCONFITTA")) {
            opponentResult = "VITTORIA";
        } else {
            System.out.println("Move result: " + result + " at [" + move.getX() + "," + move.getY() + "]");
        }

        ResultMove opponentMove = new ResultMove(move.getX(), move.getY(), opponentResult, gameOver);

        // Send appropriate messages to each client
        if (moveClient == client1) {
            client1.sendMessage(playerResult);
            client2.sendMessage(opponentMove);
        } else {
            client2.sendMessage(playerResult);
            client1.sendMessage(opponentMove);
        }

        // Handle end of game if needed
        if (gameOver) {
            System.out.println("======= GAME OVER =======");
            System.out.println("Winner: " + (moveClient == client1 ? "Player 1" : "Player 2"));
            System.out.println("Player 1 ships remaining: " + p1ShipsRemaining);
            System.out.println("Player 2 ships remaining: " + p2ShipsRemaining);
            System.out.println("=========================");
        }
    }
}
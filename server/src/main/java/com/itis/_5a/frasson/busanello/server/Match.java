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
        this.client1 = c1;
        this.client2 = c2;
        System.out.println("Creato il match");
    }

    @Override
    public void run() {
        //Al avvio del thread imosta lo stato a 2 che significa che sono entrati su un match ee indica al client che è stato trovato un match
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

    public synchronized void playerReady(ClientHandler c) {
        if (c == client1) {
            p1Ready = true;
            System.out.println("Client1 è pronto");
        }
        else if (c == client2) {
            p2Ready = true;
            System.out.println("Client2 è pronto");
        }

        if (p1Ready && p2Ready) {
            System.out.println("Inizio del game");
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
        System.out.println("Game iniziato");
    }



    public void setMapShip(int[][] map, ClientHandler c) {
        if (c == client1) {
            mapShip1 = map;
            System.out.println("Client1 ha posizionato le navi");
        } else if (c == client2) {
            mapShip2 = map;
            System.out.println("Client2 ha posizionato le navi");
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
                System.out.println("Turno iniziale del client1");
            } else if (c == client2) {
                // Client2's turn - send their ships and moves on client1's board
                client2.sendMessage(new Turn("YT", mapShip2, moveMap2));
                // Client1 waiting - send their ships and moves client2 made on their board
                client1.sendMessage(new Turn("OT", mapShip1, moveMap1));
                c1turn = false;
                System.out.println("Turno iniziale del client2");
            }
            sendTurn = true;
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

            moves[x][y] = 999;
            System.out.println("Hit water at [" + x + "," + y + "]");
            return "ACQUA";
        } else {
            moves[x][y] = shipType;
            System.out.println("Hit ship type " + shipType + " at [" + x + "," + y + "]");

            boolean isSunk = checkIfSunk(shipType, map, moves);

            if (isSunk) {
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
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                if (map[i][j] == shipType && moves[i][j] != shipType) {
                    return false;
                }
            }
        }
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
        ResultMove playerResult = new ResultMove(move.getX(), move.getY(), result, gameOver);

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

        if (moveClient == client1) {
            client1.sendMessage(playerResult);
            client2.sendMessage(opponentMove);
        } else {
            client2.sendMessage(playerResult);
            client1.sendMessage(opponentMove);
        }

        if (gameOver) {
            System.out.println("======= GAME OVER =======");
            System.out.println("Winner: " + (moveClient == client1 ? "Player 1" : "Player 2"));
            System.out.println("Player 1 ships remaining: " + p1ShipsRemaining);
            System.out.println("Player 2 ships remaining: " + p2ShipsRemaining);
            System.out.println("=========================");
        }
    }
}
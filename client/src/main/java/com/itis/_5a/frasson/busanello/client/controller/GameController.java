package com.itis._5a.frasson.busanello.client.controller;

import com.itis._5a.frasson.busanello.client.SceneManager;
import com.itis._5a.frasson.busanello.client.SocketClient;
import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.Message;
import com.itis._5a.frasson.busanello.common.Message.Move;
import com.itis._5a.frasson.busanello.common.Message.ResultMove;
import com.itis._5a.frasson.busanello.common.Message.Turn;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;

public class GameController implements Initializable {
    @FXML
    private BorderPane rootPane;
    @FXML
    private GridPane gameGrid;
    @FXML
    private GridPane opponentGrid;
    @FXML
    private Label statusLabel;

    private final int GRID_SIZE = 10;
    private Rectangle[][] myGridCells = new Rectangle[GRID_SIZE][GRID_SIZE];
    private Rectangle[][] opponentGridCells = new Rectangle[GRID_SIZE][GRID_SIZE];

    private boolean isMyTurn = false;
    private boolean gameOver = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeGrid();

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        onWindowShown();
                    }
                });
            }
        });
    }

    private void onWindowShown() {
        new Thread(() -> {
            try {
                SocketClient socketClient = SocketClient.getInstance();

                Message request = new Message("TURN");
                Turn response = socketClient.sendAndReceive(Json.serializedMessage(request), Turn.class);

                if ("OT".equals(response.getType())) {
                    isMyTurn = false;
                    Platform.runLater(() -> statusLabel.setText("Attendi l'avversario"));
                } else if ("YT".equals(response.getType())) {
                    isMyTurn = true;
                    Platform.runLater(() -> statusLabel.setText("È il tuo turno. Fai una mossa."));
                }

                setupMap(response.getShipPlace(), response.getMoves());


                while (!gameOver) {
                    if (!isMyTurn) {
                        waitForOpponentMove();

                        if (!gameOver) {
                            isMyTurn = true;
                            Platform.runLater(() -> statusLabel.setText("È il tuo turno. Fai una mossa."));
                        }
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("Game initialization error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void waitForOpponentMove() {
        try {
            SocketClient socketClient = SocketClient.getInstance();
            ResultMove move = socketClient.receiveMessage(ResultMove.class);
            if (move == null) {
                handleNetworkError();
                return;
            }
            if ("OPPONENT_DISCONNECTED".equals(move.getType())) {
                handleOpponentDisconnected();
                return;
            }
            processResultMove(move);

            if (move.isGameOver()) {

                handleGameOver(false);
            }

        } catch (Exception e) {

            System.err.println("Error waiting for opponent move: " + e.getMessage());
            e.printStackTrace();

        }
    }

    private void processResultMove(ResultMove move) {
        Platform.runLater(() -> {
            int x = move.getX();
            int y = move.getY();

            String result = move.getRmove();

            if ("VITTORIA".equals(result) || "SCONFITTA".equals(result)) {
                statusLabel.setText(result.equals("VITTORIA") ?
                        "Hai vinto la partita!" :
                        "Hai perso la partita.");
                return;
            }


            try {
                if (!isMyTurn) {
                    updateOpponentCell(x, y, result);
                    statusLabel.setText("Hai colpito: " + result);
                } else {
                    updateMyCell(x, y, result);
                    statusLabel.setText("L'avversario ha colpito: " + result);
                }
            }catch (Exception e){
                handleOpponentDisconnected();
            }
        });
    }

    private void updateMyCell(int x, int y, String result) {
        Color cellColor;
        switch (result) {
            case "ACQUA":
                cellColor = Color.CYAN;
                break;
            case "COLPITO":
                cellColor = Color.RED;
                break;
            case "AFFONDATO":
                cellColor = Color.DARKRED;
                break;
            default:
                cellColor = Color.LIGHTGRAY;
                break;
        }

        myGridCells[x][y].setFill(cellColor);
    }

    private void updateOpponentCell(int x, int y, String result) {
        Color cellColor;
        switch (result) {
            case "ACQUA":
                cellColor = Color.CYAN;
                break;
            case "COLPITO":
                cellColor = Color.RED;
                break;
            case "AFFONDATO":
                cellColor = Color.DARKRED;
                break;
            default:
                cellColor = Color.LIGHTGRAY;
                break;
        }

        opponentGridCells[x][y].setFill(cellColor);
    }

    private void handleGameOver(boolean victory) {
        gameOver = true;
        Platform.runLater(() -> {
            if (victory) {
                statusLabel.setText("Hai vinto la partita!");
            } else {
                statusLabel.setText("Hai perso la partita.");
            }

            disableGridInteractions();
showInfoAlert("Vittoria", "hai vinto");
        });
    }

    private void disableGridInteractions() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                opponentGridCells[i][j].setOnMouseClicked(null);
            }
        }
    }

    private void setupMap(int[][] shipMap, int[][] moveMap) {
        Platform.runLater(() -> {
            gameGrid.getChildren().clear();

            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {

                    Rectangle myCell = new Rectangle(30, 30);

                    if (moveMap[row][col] == 999) {
                        myCell.setFill(Color.CYAN);
                    } else if (shipMap[row][col] != 0 && moveMap[row][col] != 0) {
                        myCell.setFill(Color.RED);
                    } else if (shipMap[row][col] != 0) {
                        myCell.setFill(Color.GREEN);
                    } else {
                        myCell.setFill(Color.ALICEBLUE);
                    }

                    myCell.setStroke(Color.LIGHTGRAY);
                    myGridCells[row][col] = myCell;
                    gameGrid.add(myCell, col, row);
                    myCell.setUserData(new int[]{row, col});
                }
            }
        });
    }

    private void initializeGrid() {
        gameGrid.getChildren().clear();
        opponentGrid.getChildren().clear();

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Rectangle myCell = new Rectangle(30, 30);
                myCell.setFill(Color.ALICEBLUE);
                myCell.setStroke(Color.LIGHTGRAY);
                myGridCells[row][col] = myCell;
                gameGrid.add(myCell, col, row);
                myCell.setUserData(new int[]{row, col});

                Rectangle opponentCell = new Rectangle(30, 30);
                opponentCell.setFill(Color.ALICEBLUE);
                opponentCell.setStroke(Color.LIGHTGRAY);
                opponentGridCells[row][col] = opponentCell;
                opponentGrid.add(opponentCell, col, row);
                opponentCell.setUserData(new int[]{row, col});
                opponentCell.setOnMouseClicked(this::handleOpponentCellClick);
            }
        }
    }

    private void handleOpponentCellClick(MouseEvent event) {
        if (!isMyTurn || gameOver) {
            return;
        }

        Rectangle clickedCell = (Rectangle) event.getSource();
        int[] cellData = (int[]) clickedCell.getUserData();
        int clickedRow = cellData[0];
        int clickedCol = cellData[1];

        if (clickedCell.getFill() != Color.ALICEBLUE) {
            return;
        }

        System.out.println("Attacking: Row " + clickedRow + ", Col " + clickedCol);
        sendMove(clickedRow, clickedCol);
    }

    private void sendMove(int clickedRow, int clickedCol) {
        try {
            SocketClient socketClient = SocketClient.getInstance();
            Move move = new Move(clickedRow, clickedCol);
            socketClient.sendMessage(Json.serializedMessage(move));



            ResultMove result = socketClient.receiveMessage(ResultMove.class);


            if (result == null) {
                handleNetworkError();
                return;
            }
            if ("DISCONNECTED".equals(result.getType())) {
                handleOpponentDisconnected();
                return;
            }
            processResultMove(result);

            if (result.isGameOver()) {
                handleGameOver(true);
                return;
            }

            isMyTurn = false;
            Platform.runLater(() -> statusLabel.setText("Attendi l'avversario"));

        } catch (Exception e) {
            System.err.println("Error sending move: " + e.getMessage());
            handleNetworkError();
            e.printStackTrace();
        }
    }
    private void handleOpponentDisconnected() {
        Platform.runLater(() -> {
            statusLabel.setText("L'avversario si è disconnesso! Vittoria automatica.");
            disableGridInteractions();
            handleGameOver(true);

        });
    }

    private void handleNetworkError() {

        Platform.runLater(() -> {
            statusLabel.setText("Errore di connessione con il server");
            showErrorAlert("Errore connessione", "Errore di connesione al server");
            disableGridInteractions();
        });
    }
    public void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        SceneManager sceneManager=SceneManager.getInstance();
        sceneManager.switchTo("Main");
    }
    public void showErrorAlert(String title, String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Si è verificato un errore");
        alert.setContentText(errorMessage);
        alert.showAndWait();
        SceneManager sceneManager=SceneManager.getInstance();
        sceneManager.switchTo("Main");
    }
}
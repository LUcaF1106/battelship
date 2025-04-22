package com.itis._5a.frasson.busanello.client.controller;

import com.itis._5a.frasson.busanello.client.SceneManager;
import com.itis._5a.frasson.busanello.client.SocketClient;
import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.Message;
import com.itis._5a.frasson.busanello.common.Message.ShipPlacement;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;

public class GameController implements Initializable {
    @FXML
    private BorderPane rootPane;
    @FXML
    private GridPane gameGrid;

    private final int GRID_SIZE = 10;
    private Rectangle[][] gridCells = new Rectangle[GRID_SIZE][GRID_SIZE];

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
                SceneManager sceneManager = SceneManager.getInstance();

                Message request = new Message("TURN");
                Message response = socketClient.sendAndReceive(Json.serializedMessage(request), Message.class);

                if("OT".equals(response.getType())){
                    System.out.println("Turno avversario");
                } else if ("YT".equals(response.getType())) {
                    System.out.println("Mio turno");
                }

            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }).start();
    }
    private void initializeGrid() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Rectangle cell = new Rectangle(30, 30);
                cell.setFill(Color.ALICEBLUE);
                cell.setStroke(Color.LIGHTGRAY);

                gridCells[row][col] = cell;

                gameGrid.add(cell, col, row);


                cell.setUserData(new int[]{row, col});

            }
        }
    }
}

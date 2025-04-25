package com.itis._5a.frasson.busanello.client.controller;

import com.itis._5a.frasson.busanello.client.SceneManager;
import com.itis._5a.frasson.busanello.client.SocketClient;
import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.Message;
import com.itis._5a.frasson.busanello.common.Message.ShipPlacement;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class ShipPlacementController implements Initializable {
    private static final Logger logger = LogManager.getLogger(ShipPlacementController.class);

    @FXML private GridPane gameGrid;
    @FXML private VBox shipsContainer;
    @FXML private Label statusLabel;
    @FXML private Button rotateButton;
    @FXML private Button submit;
    @FXML private Button resetButton;

    @FXML private AnchorPane ship1;
    @FXML private AnchorPane ship2;
    @FXML private AnchorPane ship3;
    @FXML private AnchorPane ship4;
    @FXML private AnchorPane ship5;

    private final int GRID_SIZE = 10;
    private Rectangle[][] gridCells = new Rectangle[GRID_SIZE][GRID_SIZE];
    private int[][] shipMatrix = new int[GRID_SIZE][GRID_SIZE]; // 0 = acqua, 1-5 = nave

    private AnchorPane selectedShip = null;
    private boolean isVertical = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);
        logger.info("Initializing ship placement controller");

        initializeGrid();
        submit.setDisable(true);

        //inizializza il dragAndDrop per ogni navae
        setupDragAndDrop(ship1);
        setupDragAndDrop(ship2);
        setupDragAndDrop(ship3);
        setupDragAndDrop(ship4);
        setupDragAndDrop(ship5);

        //inizializza la matrice delle navi da inviare al server
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                shipMatrix[i][j] = 0;
            }
        }
    }


    private void initializeGrid() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Rectangle cell = new Rectangle(30, 30);
                cell.setFill(Color.ALICEBLUE);
                cell.setStroke(Color.LIGHTGRAY);

                gridCells[row][col] = cell;


                //lo aggiuge alla grafica
                gameGrid.add(cell, col, row);


                cell.setUserData(new int[]{row, col});

                //rende possilie il drag and drop nella cella
                setupCellDAD(cell);
            }
        }
    }

    private void setupDragAndDrop(AnchorPane ship) {
        ship.setOnDragDetected(event -> {
            if (!ship.isDisable()) {
                selectedShip = ship;

                Dragboard db = ship.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(ship.getId());
                db.setContent(content);

                event.consume();
            }
        });
    }

    private void setupCellDAD(Rectangle cell) {
        //controlla cosa viene spostato sopra la cella e se è una nava
        cell.setOnDragOver(event -> {
            if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                if (canPlaceShip(cell, selectedShip)) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
            }
            event.consume(); //blocca propagazione ad altri
        });

        //entra nella cella e controlla se puo essere posizionato o meno
        cell.setOnDragEntered(event -> {
            if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                if (canPlaceShip(cell, selectedShip)) {
                    highlightCells(cell, selectedShip, Color.LIGHTGREEN);
                } else {
                    highlightCells(cell, selectedShip, Color.LIGHTPINK);
                }
            }
            event.consume();
        });

        //resetta il colore
        cell.setOnDragExited(event -> {

            resetHighlight();
            event.consume();
        });

        //gestisce cosa succede dopo il rilascio
        cell.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasString() && canPlaceShip(cell, selectedShip)) {
                int[] coords = (int[]) cell.getUserData();
                int row = coords[0];
                int col = coords[1];
                int size = Integer.parseInt(selectedShip.getUserData().toString());


                placeShipInMatrix(row, col, size);

                placeShipOnGrid(row, col, size);


                selectedShip.setDisable(true);
                selectedShip.setOpacity(0.5);

                success = true;


                checkAllShipsPlaced();
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean canPlaceShip(Rectangle cell, AnchorPane ship) {
        if (ship == null) return false;

        int[] coords = (int[]) cell.getUserData();
        int row = coords[0];
        int col = coords[1];
        int size = Integer.parseInt(ship.getUserData().toString());

        if (isVertical) {
            if (row + size > GRID_SIZE) return false;
        } else {
            if (col + size > GRID_SIZE) return false;
        }


        for (int i = 0; i < size; i++) {
            int checkRow = isVertical ? row + i : row;
            int checkCol = isVertical ? col : col + i;

            if (shipMatrix[checkRow][checkCol] != 0) {
                return false;
            }
        }

        return true;
    }

    private void highlightCells(Rectangle cell, AnchorPane ship, Color color) {
        if (ship == null) return;

        int[] coords = (int[]) cell.getUserData();
        int row = coords[0];
        int col = coords[1];
        int size = Integer.parseInt(ship.getUserData().toString());

        for (int i = 0; i < size; i++) {

            int highlightRow =  row;
            int highlightCol =  col;
            if(isVertical){
                highlightRow+=i;

            }else{
                highlightCol+=i;
            }



            if (highlightRow < GRID_SIZE && highlightCol < GRID_SIZE) {
                gridCells[highlightRow][highlightCol].setFill(color);
            }
        }
    }

    private void resetHighlight() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (shipMatrix[i][j] == 0) {
                    gridCells[i][j].setFill(Color.ALICEBLUE);
                }
                else{
                    gridCells[i][j].setFill(getShipColorSize(shipMatrix[i][j]));
                }
            }
        }
    }

    private void placeShipInMatrix(int row, int col, int size) {
        logger.debug("Placing ship of size " + size + " at position (" + row + "," + col + "), vertical: " + isVertical);


        for (int i = 0; i < size; i++) {
            int placeRow =  row;
            int placeCol =  col;
            if(isVertical){

                placeRow+=i;

            }else{
                placeCol+=i;
            }
            shipMatrix[placeRow][placeCol] = size;
        }
    }

    private void placeShipOnGrid(int row, int col, int size) {

        Color shipColor = getShipColor(selectedShip);

        for (int i = 0; i < size; i++) {
            int placeRow =  row;
            int placeCol =  col;
            if(isVertical){
                placeRow+=i;

            }else{
                placeCol+=i;
            }


            gridCells[placeRow][placeCol].setFill(shipColor);
            gridCells[placeRow][placeCol].setStroke(Color.BLACK);
        }
    }

   private Color getShipColor(AnchorPane ship) {
        if (ship == ship1) return Color.DARKBLUE;
        if (ship == ship2) return Color.NAVY;
        if (ship == ship3) return Color.ROYALBLUE;
        if (ship == ship4) return Color.SKYBLUE;
        if (ship == ship5) return Color.LIGHTBLUE;
        return Color.GRAY;
    }
    private Color getShipColorSize(int ship) {
        if (ship == 5) return Color.DARKBLUE;
        if (ship == 4) return Color.NAVY;
        if (ship == 3) return Color.ROYALBLUE;
        if (ship == 2) return Color.SKYBLUE;
        if (ship == 1) return Color.LIGHTBLUE;
        return Color.GRAY;
    }

    private void checkAllShipsPlaced() {
        boolean allPlaced = ship1.isDisable() && ship2.isDisable() &&
                ship3.isDisable() && ship4.isDisable() && ship5.isDisable();

        if (allPlaced) {
            statusLabel.setText("Tutte le navi sono state posizionate!");

            submit.setDisable(false);
        }
    }

    @FXML
    private void rotateShip() {
        isVertical = !isVertical;
        rotateButton.setText(isVertical ? "Orizzontale" : "Verticale");

    }

    @FXML
    private void resetGame() {
        logger.info("Resetting ship placement");

        // Resetta la matrice
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                shipMatrix[i][j] = 0;
                gridCells[i][j].setFill(Color.ALICEBLUE);
                gridCells[i][j].setStroke(Color.LIGHTGRAY);
            }
        }

        // Riabilita tutte le navi
        ship1.setDisable(false);
        ship2.setDisable(false);
        ship3.setDisable(false);
        ship4.setDisable(false);
        ship5.setDisable(false);

        ship1.setOpacity(1.0);
        ship2.setOpacity(1.0);
        ship3.setOpacity(1.0);
        ship4.setOpacity(1.0);
        ship5.setOpacity(1.0);

        statusLabel.setText("Posiziona le tue navi sulla griglia");



        isVertical = false;
        rotateButton.setText("Verticale");

        submit.setDisable(true);

    }
    public void submit(){
        SocketClient socketClient=SocketClient.getInstance();
        logger.info("Submitting ship placement to server");

        Message message;
        statusLabel.setText("In attesa dell'altro giocatore");
        resetButton.setDisable(true);

        submit.setDisable(true);

        rotateButton.setDisable(true);
        try {

           message= socketClient.sendAndReceive(Json.serializedMessage(new ShipPlacement(shipMatrix)), Message.class);
           logger.info("Ship placement accepted, moving to game");

        } catch (Exception e) {
            logger.error("Error submitting ship placement: " + e.getMessage(), e);

            throw new RuntimeException(e);
        }
        if("PLAY".equals(message.getType())){
            SceneManager sceneManager =SceneManager.getInstance();
            sceneManager.switchTo("Play");


        }

    }
}

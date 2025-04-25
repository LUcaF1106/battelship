package com.itis._5a.frasson.busanello.client.controller;

import com.itis._5a.frasson.busanello.client.SceneManager;
import com.itis._5a.frasson.busanello.client.SocketClient;
import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class LoadingController {
    private static final Logger logger = LogManager.getLogger(LoadingController.class);

    @FXML
    private StackPane rootPane;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    public void initialize() {
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);

        progressIndicator.setMinSize(100, 100);
        progressIndicator.setPrefSize(100, 100);
        progressIndicator.setMaxSize(100, 100);


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
        logger.info("Loading screen shown, searching for match");

        new Thread(() -> {
            try {
                SocketClient socketClient = SocketClient.getInstance();
                SceneManager sceneManager = SceneManager.getInstance();

                Message request = new Message("FMATCH");
                Message response = socketClient.sendAndReceive(Json.serializedMessage(request), Message.class);
                logger.info("Match found, proceeding to ship placement");


                Platform.runLater(() -> {
                    if (response != null && "MFIND".equals(response.getType())) {
                        sceneManager.switchTo("ShipPlacement");
                    } else {

                        sceneManager.switchTo("MainMenu");
                        logger.info("Match not found, returning to main menu");

                    }
                });
            } catch (Exception e) {
                logger.error("Error finding match: " + e.getMessage(), e);

            }
        }).start();
    }

}
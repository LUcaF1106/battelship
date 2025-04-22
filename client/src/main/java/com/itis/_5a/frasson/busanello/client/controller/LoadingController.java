package com.itis._5a.frasson.busanello.client.controller;

import com.itis._5a.frasson.busanello.client.SceneManager;
import com.itis._5a.frasson.busanello.client.SocketClient;
import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

public class LoadingController {
    @FXML
    private StackPane rootPane;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    public void initialize() {

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

        new Thread(() -> {
            try {
                SocketClient socketClient = SocketClient.getInstance();
                SceneManager sceneManager = SceneManager.getInstance();

                Message request = new Message("FMATCH");
                Message response = socketClient.sendAndReceive(Json.serializedMessage(request), Message.class);


                Platform.runLater(() -> {
                    if (response != null && "MFIND".equals(response.getType())) {
                        sceneManager.switchTo("ShipPlacement");
                    } else {

                        sceneManager.switchTo("MainMenu"); // Assuming you have a main menu scene
                    }
                });
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }).start();
    }

}
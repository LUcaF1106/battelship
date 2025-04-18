package com.itis._5a.frasson.busanello.client.controller;

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
    }
}
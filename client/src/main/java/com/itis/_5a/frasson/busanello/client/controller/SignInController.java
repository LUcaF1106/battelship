package com.itis._5a.frasson.busanello.client.controller;

import com.itis._5a.frasson.busanello.client.SceneManager;
import com.itis._5a.frasson.busanello.client.SocketClient;
import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.Message;
import com.itis._5a.frasson.busanello.common.Message.SignUpM;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class SignInController {
    private static final Logger logger = LogManager.getLogger(SignInController.class);

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private void handleSignIn() {
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);

        logger.info("Sign up attempt for user: " + usernameField.getText());

        if (usernameField.getText().isEmpty() || passwordField.getText().isEmpty() || confirmPasswordField.getText().isEmpty()) {
            messageLabel.setText("Please fill in all fields");
            messageLabel.setStyle("-fx-text-fill: red;");
            logger.warn("Sign up validation failed: " + messageLabel.getText());

            return;
        }


        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            messageLabel.setText("Passwords do not match");
            messageLabel.setStyle("-fx-text-fill: red;");
            logger.warn("Sign up validation failed: " + messageLabel.getText());

            return;
        }

        SocketClient socketClient = SocketClient.getInstance();
        if (socketClient != null && socketClient.isIsconnected()) {
            try {
                SignUpM signUpMessage = new SignUpM();
                signUpMessage.setUser(usernameField.getText());
                signUpMessage.setPassword(passwordField.getText());

                Message response = socketClient.sendAndReceive(Json.serializedMessage(signUpMessage), Message.class);

                if ("ACC".equals(response.getType())) {
                    messageLabel.setText("Registration successful!");
                    messageLabel.setStyle("-fx-text-fill: green;");
                    logger.info("Registration successful for user: " + usernameField.getText());

                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            javafx.application.Platform.runLater(() -> {
                                SceneManager sm = SceneManager.getInstance();
                                sm.switchTo("Login");
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    logger.warn("Registration failed for user: " + usernameField.getText() + ": " + response.getType());

                    messageLabel.setText("Registration failed: " + response.getType());
                    messageLabel.setStyle("-fx-text-fill: red;");
                }
            } catch (Exception e) {
                logger.error("Error during registration: " + e.getMessage(), e);

                messageLabel.setText("Error during registration: " + e.getMessage());
                messageLabel.setStyle("-fx-text-fill: red;");
                e.printStackTrace();
            }
        } else {
            messageLabel.setText("Not connected to server");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleBackToLogin() {
        SceneManager sm = SceneManager.getInstance();
        sm.switchTo("Login");
    }
}
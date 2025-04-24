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

public class SignInController {
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
        // Basic validation
        if (usernameField.getText().isEmpty() || passwordField.getText().isEmpty() || confirmPasswordField.getText().isEmpty()) {
            messageLabel.setText("Please fill in all fields");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Check if passwords match
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            messageLabel.setText("Passwords do not match");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        SocketClient socketClient = SocketClient.getInstance();
        if (socketClient != null && socketClient.isIsconnected()) {
            try {
                // Create sign up message
                SignUpM signUpMessage = new SignUpM();
                signUpMessage.setUser(usernameField.getText());
                signUpMessage.setPassword(passwordField.getText());

                // Send to server and get response
                Message response = socketClient.sendAndReceive(Json.serializedMessage(signUpMessage), Message.class);

                if ("ACC_REG".equals(response.getType())) {
                    messageLabel.setText("Registration successful!");
                    messageLabel.setStyle("-fx-text-fill: green;");

                    // Navigate back to login after short delay
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
                    messageLabel.setText("Registration failed: " + response.getType());
                    messageLabel.setStyle("-fx-text-fill: red;");
                }
            } catch (Exception e) {
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
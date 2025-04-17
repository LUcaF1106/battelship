package com.itis._5a.frasson.busanello.client;

import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.LoginM;
import com.itis._5a.frasson.busanello.common.Message.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;
    @FXML private Button connectButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        updateMessage("Initializing...", "blue");
    }

    public void setConnectionStatus(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                updateMessage("Connected to server", "green");
                connectButton.setDisable(true);
                loginButton.setDisable(false);
            } else {
                updateMessage("Disconnected from server", "red");
                connectButton.setDisable(false);
                loginButton.setDisable(true);
            }
        });
    }

    @FXML
    private void handleConnectToServer() {
        updateMessage("Connecting...", "blue");

        new Thread(() -> {
            try {
                SocketClient socketClient = SocketClient.getInstance();
                boolean connected = socketClient.connect(Client.SERVER_HOST, Client.SERVER_PORT, Client.CONNECTION_TIMEOUT);

                Platform.runLater(() -> {
                    if (connected) {
                        updateMessage("Connected successfully", "green");
                    } else {
                        updateMessage("Connection failed", "red");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() ->
                        updateMessage("Connection error: " + e.getMessage(), "red")
                );
            }
        }).start();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            updateMessage("Username and password required", "red");
            return;
        }

        updateMessage("Authenticating...", "blue");

        new Thread(() -> {
            try {
                LoginM loginMessage = new LoginM();
                loginMessage.setUser(username);
                loginMessage.setPassword(password);

                Message response = SocketClient.getInstance().sendAndReceive(
                        Json.serializedMessage(loginMessage),
                        Message.class
                );

                Platform.runLater(() -> {
                    if ("ACC".equals(response.getType())) {
                        updateMessage("Login successful!", "green");
                        loadMainScreen(true);
                    } else {
                        updateMessage("Invalid credentials", "red");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    updateMessage("Communication error: " + e.getMessage(), "red");
                    setConnectionStatus(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleWithoutLogin() {
        loadMainScreen(false);
    }

    private void updateMessage(String message, String color) {
        Platform.runLater(() -> {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: " + color + ";");
        });
    }

    private void loadMainScreen(boolean authenticated) {
        try {
            ClientInfo.getInstance().setValue(authenticated);

            Platform.runLater(() -> {
                try {
                    Stage currentStage = (Stage) loginButton.getScene().getWindow();
                    Parent root = FXMLLoader.load(getClass().getResource("/MainPage.fxml"));
                    currentStage.setScene(new Scene(root));
                    currentStage.setTitle("Battaglia Navale");
                    currentStage.setMaximized(true);
                } catch (IOException e) {
                    updateMessage("Error loading main screen", "red");
                }
            });
        } catch (Exception e) {
            updateMessage("Error: " + e.getMessage(), "red");
        }
    }
}
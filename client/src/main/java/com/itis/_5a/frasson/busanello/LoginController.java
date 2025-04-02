package com.itis._5a.frasson.busanello;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        SocketClient socketClient = SocketClient.getInstance();
        if (socketClient != null && socketClient.isConnected()) {
            String loginMessage = "login:" + username + ":" + password;
            String sent = socketClient.sendAndReceive(loginMessage);

            if (sent.equals("Accesso")) {
                messageLabel.setText("Credenziali inviate al server!");
                messageLabel.setStyle("-fx-text-fill: green;");

            } else {
                messageLabel.setText("Errore nell'invio delle credenziali");
                messageLabel.setStyle("-fx-text-fill: red;");
            }
        } else {
            messageLabel.setText("Non connesso al server");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

}

package com.itis._5a.frasson.busanello.client.controller;
import com.itis._5a.frasson.busanello.client.ClientInfo;
import com.itis._5a.frasson.busanello.client.SceneManager;
import com.itis._5a.frasson.busanello.client.SocketClient;
import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.LoginM;
import com.itis._5a.frasson.busanello.common.Message.Message;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class LoginController {
    private static final Logger logger = LogManager.getLogger(LoginController.class);

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    @FXML
    private void handleLogin() throws Exception {
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);
        logger.info("Login attempt for user: " + usernameField.getText());


        SocketClient socketClient = SocketClient.getInstance();
        if (socketClient != null && socketClient.isIsconnected()) {

            LoginM mes=new LoginM();

            mes.setUser( usernameField.getText());
            mes.setPassword(passwordField.getText());

            Message sent=socketClient.sendAndReceive(Json.serializedMessage(mes), Message.class);


            if (sent.getType().equals("ACC")) {
                messageLabel.setText("Credenziali inviate al server!");
                logger.info("Login successful for user: " + usernameField.getText());
                messageLabel.setStyle("-fx-text-fill: green;");
                loadMainScreen(true);

            } else {
                messageLabel.setText("Errore nell'invio delle credenziali");
                messageLabel.setStyle("-fx-text-fill: red;");
            }
        } else {
            logger.warn("Login failed for user: " + usernameField.getText());

            messageLabel.setText("Non connesso al server");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }
    @FXML
    private void hadleWithoutLogin(){
        logger.info("User proceeding without login");

        loadMainScreen(false);
    }

    @FXML
    private void handleSignIn() {
        SceneManager sm = SceneManager.getInstance();
        sm.switchTo("SignIn");
    }

    private void loadMainScreen(boolean auth) {
        ClientInfo clientInfo=ClientInfo.getInstance();
        clientInfo.setValue(auth);

        SceneManager sm= SceneManager.getInstance();

        sm.switchTo("Main");

    }

}

package com.itis._5a.frasson.busanello.client;
import com.google.gson.Gson;
import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.LoginM;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
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


        SocketClient socketClient = SocketClient.getInstance();
        if (socketClient != null && socketClient.isConnected()) {

            LoginM mes=new LoginM();
            mes.setUser( usernameField.getText());
            mes.setPassword(passwordField.getText());

            String sent = socketClient.sendAndReceive(Json.serializedMessage(mes));
            System.out.println(sent);
            System.out.println("ciao");
            if (sent.equals("Accesso")) {
                messageLabel.setText("Credenziali inviate al server!");
                messageLabel.setStyle("-fx-text-fill: green;");
                loadMainScreen(true);

            } else {
                messageLabel.setText("Errore nell'invio delle credenziali");
                messageLabel.setStyle("-fx-text-fill: red;");
            }
        } else {
            messageLabel.setText("Non connesso al server");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }
    @FXML
    private void hadleWithoutLogin(){
        loadMainScreen(false);
    }

    private void loadMainScreen(boolean auth) {
        try {
            ClientInfo clientInfo=ClientInfo.getInstance();
            clientInfo.setValue(auth);
            Stage currentStage = (Stage) loginButton.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainPage.fxml"));
            Parent root = loader.load();


            Scene scene = new Scene(root);

            currentStage.setScene(scene);
            currentStage.setTitle("Battaglia Navale");

            currentStage.setFullScreen(true);


        } catch (IOException e) {
            e.printStackTrace();

        }
    }

}

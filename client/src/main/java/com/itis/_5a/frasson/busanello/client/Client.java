package com.itis._5a.frasson.busanello.client;

import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.Message;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Client extends Application {


    private final String[][] scenes = {
            {"Login", "/LoginPage.fxml"},
            {"SignIn", "/SignInPage.fxml"},
            {"Main", "/MainPage.fxml"},
            {"Loading", "/LoadingPage.fxml"},
            {"ShipPlacement", "/ShipPlacementPage.fxml"},
            {"Play", "/GamePage.fxml"}
    };

    private SocketClient socketClient;
    private Thread socketThread;


    @Override
    public void start(Stage primaryStage) throws Exception {

        primaryStage.setTitle("Socket Application");
        primaryStage.setOnCloseRequest(windowEvent -> {
            SocketClient sc= SocketClient.getInstance();
            sc.disconnect();
            socketThread.interrupt();
            Thread.currentThread().interrupt();
        });
        SceneManager sceneManager=SceneManager.getInstance();
        sceneManager.setStage(primaryStage);


        for (String[] scene : scenes) {
            sceneManager.addScene(scene[0], scene[1]);
        }



        initSocketConnection();
        sceneManager.switchTo("Login");

    }

    @Override
    public void stop() throws Exception {
        closeSocket();

    }
    private void initSocketConnection() {


        socketThread = new Thread(SocketClient.getInstance());

        socketThread.setDaemon(true);
        socketThread.start();
    }

    private void closeSocket() throws Exception {
        if (socketClient != null && socketClient.isIsconnected()) {
            socketClient.disconnect();
        }

        if (socketThread != null && socketThread.isAlive()) {
            socketThread.interrupt();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
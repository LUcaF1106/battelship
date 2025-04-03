package com.itis._5a.frasson.busanello.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Client extends Application {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private SocketClient socketClient;
    private Thread socketThread;


    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginPage.fxml"));
        Parent root = loader.load();
        initSocketConnection();
        primaryStage.setTitle("Socket Application");
        primaryStage.setScene(new Scene(root, 900, 500));
        primaryStage.show();
    }

    @Override
    public void stop() throws IOException{
        closeSocket();

    }
    private void initSocketConnection() {
        socketClient = SocketClient.getInstance();


        socketThread = new Thread(() -> {
            System.out.println("Thread socket avviato - Tentativo di connessione a " + SERVER_HOST + ":" + SERVER_PORT);
            boolean connected = socketClient.connect(SERVER_HOST, SERVER_PORT);

            if (connected) {
                System.out.println("Connessione al server stabilita");

            } else {
                System.err.println("Impossibile connettersi al server");
            }
        });

        socketThread.setDaemon(true);
        socketThread.setName("SocketConnectionThread");
        socketThread.start();
    }

    private void closeSocket() throws IOException {
        if (socketClient != null && socketClient.isConnected()) {
            socketClient.sendMessage("logout");
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
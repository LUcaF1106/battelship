package com.itis._5a.frasson.busanello.client;

import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.Message.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Client extends Application {
    public static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 12345;
    public static final int CONNECTION_TIMEOUT = 5000; // 5 seconds

    private SocketClient socketClient;
    private Thread socketThread;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        // Load the login page first
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginPage.fxml"));
        Parent root = loader.load();

        // Setup primary stage
        primaryStage.setTitle("Battaglia Navale - Login");
        primaryStage.setScene(new Scene(root, 900, 500));
        primaryStage.show();

        // Initialize connection after UI is shown
        initSocketConnection(loader.getController());
    }

    @Override
    public void stop() {
        closeSocket();
        System.out.println("Application shutting down");
    }

    private void initSocketConnection(LoginController controller) {
        socketClient = SocketClient.getInstance();

        socketThread = new Thread(() -> {
            try {
                System.out.println("Attempting connection to " + SERVER_HOST + ":" + SERVER_PORT);
                boolean connected = socketClient.connect(SERVER_HOST, SERVER_PORT, CONNECTION_TIMEOUT);

                Platform.runLater(() -> {
                    controller.setConnectionStatus(connected);
                    if (connected) {
                        System.out.println("Server connection established");
                    } else {
                        System.err.println("Failed to connect to server");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> controller.setConnectionStatus(false));
                System.err.println("Connection error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        socketThread.setDaemon(true);
        socketThread.setName("SocketConnectionThread");
        socketThread.start();
    }

    private void closeSocket() {
        try {
            if (socketClient != null && socketClient.isConnected()) {
                socketClient.sendMessage(Json.serializedMessage(new Message("LOGOUT")));
                socketClient.disconnect();
            }
        } catch (Exception e) {
            System.err.println("Error during socket close: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
package com.itis._5a.frasson.busanello.client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Setter;

import java.util.HashMap;

public class SceneManager {
    private static SceneManager instance;
    @Setter
    private Stage stage;
    private HashMap<String, Scene> scenes = new HashMap<>();
    private final static int S_WIDTH=1000;
    private final static int S_HEIGHT=700;


    public static synchronized SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    public SceneManager() {

    }

    public void addScene(String name, String fxmlPath) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        scenes.put(name, new Scene(root, S_WIDTH, S_HEIGHT));
    }

    public void switchTo(String name) {
        stage.setScene(scenes.get(name));
        stage.show();
    }
}


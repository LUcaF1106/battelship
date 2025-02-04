package com.itis._5a.frasson.busanello;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;


public class Client extends Application {


    private static final Logger logger = LogManager.getLogger(Client.class);

    @Override
    public void start(Stage stage) {
        var javaVersion = "SystemInfo.javaVersion()";
        var javafxVersion = "SystemInfo.javafxVersion()";

        var label = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
        var scene = new Scene(new StackPane(label), 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);
        logger.info("'ciao'");
        launch();
        
    }

}
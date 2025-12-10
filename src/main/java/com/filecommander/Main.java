package com.filecommander;

import com.filecommander.repository.DatabaseManager;
import com.filecommander.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        DatabaseManager.getInstance();

        try {
            primaryStage.getIcons().add(new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/icon.png")
            ));
        } catch (Exception e) {
            System.err.println("Icon not found: " + e.getMessage());
        }

        primaryStage.setTitle("File Commander");

        MainWindow mainWindow = new MainWindow();
        mainWindow.start(primaryStage);
    }

    @Override
    public void stop() {
        DatabaseManager.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
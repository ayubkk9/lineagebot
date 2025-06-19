package com.lineagebot;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/BotUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 1100);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Lineage II Bot");
        primaryStage.setResizable(false);
        BotUIController controller = fxmlLoader.getController();
        controller.setPrimaryStage(primaryStage);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
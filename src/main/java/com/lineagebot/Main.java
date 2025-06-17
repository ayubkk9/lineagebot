package com.lineagebot;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/BotUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        BotUIController controller = fxmlLoader.getController();
        controller.setPrimaryStage(primaryStage);

        primaryStage.setTitle("Lineage Bot");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
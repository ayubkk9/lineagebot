package com.lineagebot;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.bytedeco.javacpp.Loader;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        Loader.load(org.bytedeco.opencv.global.opencv_core.class);
        Loader.load(org.bytedeco.openblas.global.openblas_nolapack.class);

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/BotUI.fxml"));
        if (fxmlLoader.getLocation() == null) {
            throw new IOException("Не удалось найти BotUI.fxml в ресурсах");
        }
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
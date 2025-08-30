package com.lineagebot;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LicenseDialog {
    private final LicenseManager licenseManager;
    private final Stage primaryStage;
    private boolean licenseValid = false;

    public LicenseDialog(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.licenseManager = new LicenseManager();
    }

    public boolean showLicenseDialog() {
        // Если лицензия уже валидна, сразу возвращаем true
        if (licenseManager.isValid()) {
            return true;
        }

        // Создаем модальное окно, которое блокирует основное приложение
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Активация лицензии");
        dialog.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Активация продукта");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label infoLabel = new Label("Для использования бота требуется лицензия на 30 дней");
        infoLabel.setWrapText(true);

        TextField licenseField = new TextField();
        licenseField.setPromptText("Введите ключ лицензии");
        licenseField.setMaxWidth(300);

        Button activateButton = new Button("Активировать");
        activateButton.setDefaultButton(true);

        Button cancelButton = new Button("Отмена");

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: red;");

        // Обработчик активации
        activateButton.setOnAction(e -> {
            String key = licenseField.getText().trim();
            if (key.isEmpty()) {
                statusLabel.setText("Введите ключ лицензии");
                return;
            }

            // Проверяем, не использовался ли ключ
            if (licenseManager.isKeyUsed(key)) {
                statusLabel.setText("❌ Ключ уже использовался");
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            if (licenseManager.activateLicense(key)) {
                licenseValid = true;
                statusLabel.setText("✅ Лицензия активирована!");
                statusLabel.setStyle("-fx-text-fill: green;");

                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    Platform.runLater(dialog::close);
                }).start();
            } else {
                statusLabel.setText("❌ Неверный ключ лицензии");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        });

        // Обработчик отмены
        cancelButton.setOnAction(e -> {
            licenseValid = false;
            dialog.close();
        });

        root.getChildren().addAll(
                titleLabel, infoLabel, licenseField,
                new HBox(10, activateButton, cancelButton), statusLabel
        );

        Scene scene = new Scene(root, 400, 250);
        dialog.setScene(scene);

        // Ждем закрытия окна
        dialog.showAndWait();

        return licenseValid;
    }

    public LicenseManager getLicenseManager() {
        return licenseManager;
    }
}
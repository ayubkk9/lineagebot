package com.lineagebot;

import com.fazecast.jSerialComm.SerialPort;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;

public class BotUIController {
    @FXML private ComboBox<String> characterComboBox;
    @FXML private Button refreshCharactersButton;
    @FXML private Button activateWindowButton;
    @FXML private TextField hpDisplayField;
    @FXML private TextField mpDisplayField;
    @FXML private TextField hpPercentField;
    @FXML private TextField mpPercentField;
    @FXML private TextField hpBarField;
    @FXML private TextField mpBarField;
    @FXML private TextField mobHpBarField;
    @FXML private ComboBox<String> arduinoPortComboBox;
    @FXML private TableView<Action> actionsTable;
    @FXML private TableColumn<Action, String> actionTypeColumn;
    @FXML private TableColumn<Action, String> keysColumn;
    @FXML private ComboBox<String> actionTypeComboBox;
    @FXML private TextField keysField;
    @FXML private Button addActionButton;
    @FXML private Button editActionButton;
    @FXML private Button deleteActionButton;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private TextArea logArea;
    @FXML private Button selectMobHpBarButton;
    @FXML private Button selectHpBarButton;
    @FXML private Button selectMpBarButton;
    @FXML private Button saveSettingsButton;
    @FXML private Button loadSettingsButton;

    private BotController botController;
    private Thread hpMpUpdateThread;
    private final ObservableList<Action> actions = FXCollections.observableArrayList();
    private List<String> capturedKeys = new ArrayList<>();
    private Action editingAction;
    private Stage primaryStage;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public static class Action {
        private final SimpleStringProperty actionType;
        private final SimpleStringProperty keys;

        public Action(String actionType, String keys) {
            this.actionType = new SimpleStringProperty(actionType);
            this.keys = new SimpleStringProperty(keys);
        }

        public String getActionType() {
            return actionType.get();
        }

        public String getKeys() {
            return keys.get();
        }

        public void setActionType(String actionType) {
            this.actionType.set(actionType);
        }

        public void setKeys(String keys) {
            this.keys.set(keys);
        }

        public SimpleStringProperty actionTypeProperty() {
            return actionType;
        }

        public SimpleStringProperty keysProperty() {
            return keys;
        }
    }

    @FXML
    private void initialize() {
        stopButton.setDisable(true);
        actionTypeColumn.setCellValueFactory(cellData -> cellData.getValue().actionTypeProperty());
        keysColumn.setCellValueFactory(cellData -> cellData.getValue().keysProperty());
        actionsTable.setItems(actions);
        actionTypeComboBox.getItems().addAll("Атака моба", "Моб убит", "Низкое HP", "Низкое MP", "Поиск Моба");
        hpPercentField.setText("30");
        mpPercentField.setText("30");
        hpBarField.setText("50,50,100,10");
        mpBarField.setText("50,80,100,10");
        mobHpBarField.setText("200,100,50,10");
        refreshCharacters();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            arduinoPortComboBox.getItems().add(port.getSystemPortName());
        }
        if (!arduinoPortComboBox.getItems().isEmpty()) {
            arduinoPortComboBox.getSelectionModel().selectFirst();
        }

        keysField.setOnKeyPressed(this::handleKeyPress);
        keysField.setOnMouseClicked(event -> {
            capturedKeys.clear();
            keysField.setText("");
            keysField.requestFocus();
        });
    }

    private void handleKeyPress(KeyEvent event) {
        KeyCode keyCode = event.getCode();
        String key = mapKeyCodeToString(keyCode);
        if (key != null && !capturedKeys.contains(key)) {
            capturedKeys.add(key);
            keysField.setText(String.join(",", capturedKeys));
        }
        event.consume();
    }

    private String mapKeyCodeToString(KeyCode keyCode) {
        switch (keyCode) {
            case F1: return "F1";
            case F2: return "F2";
            case F3: return "F3";
            case F4: return "F4";
            case F5: return "F5";
            case F6: return "F6";
            case F7: return "F7";
            case F8: return "F8";
            case F9: return "F9";
            case F10: return "F10";
            case F11: return "F11";
            case F12: return "F12";
            case TAB: return "TAB";
            case ENTER: return "ENTER";
            case SPACE: return "SPACE";
            default:
                String name = keyCode.getName();
                if (name.length() == 1 && Character.isLetterOrDigit(name.charAt(0))) {
                    return name.toUpperCase();
                }
                return null;
        }
    }

    @FXML
    private void refreshCharacters() {
        characterComboBox.getItems().clear();
        User32.INSTANCE.EnumWindows((hWnd, arg) -> {
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hWnd, windowText, 512);
            String title = new String(windowText).trim();
            if (!title.isEmpty()) {
                System.out.println("Обнаружено окно: " + title);
                Platform.runLater(() -> {
                    if (!characterComboBox.getItems().contains(title)) {
                        characterComboBox.getItems().add(title);
                    }
                });
            }
            return true;
        }, null);
        Platform.runLater(() -> {
            if (!characterComboBox.getItems().isEmpty()) {
                characterComboBox.getSelectionModel().selectFirst();
            } else if (botController != null) {
                botController.log("Окна Lineage 2 не найдены. Убедитесь, что игра запущена.");
            }
        });
    }

    @FXML
    private void activateWindow() {
        String selectedCharacter = characterComboBox.getSelectionModel().getSelectedItem();
        if (selectedCharacter != null) {
            WinDef.HWND hWnd = User32.INSTANCE.FindWindow(null, selectedCharacter);
            if (hWnd != null) {
                User32.INSTANCE.SetForegroundWindow(hWnd);
                if (botController != null) {
                    botController.log("Активировано окно: " + selectedCharacter);
                }
            } else if (botController != null) {
                botController.log("Окно " + selectedCharacter + " не найдено");
            }
        } else if (botController != null) {
            botController.log("Персонаж не выбран");
        }
    }

    @FXML
    private void addAction() {
        String actionType = actionTypeComboBox.getSelectionModel().getSelectedItem();
        String keys = keysField.getText();
        if (actionType != null && !keys.isEmpty()) {
            if (editingAction != null) {
                editingAction.setActionType(actionType);
                editingAction.setKeys(keys);
                editingAction = null;
                editActionButton.setText("Редактировать");
            } else {
                actions.add(new Action(actionType, keys));
            }
            clearActionFields();
        } else if (botController != null) {
            botController.log("Ошибка: тип действия или клавиши не выбраны");
        }
    }

    @FXML
    private void editAction() {
        Action selectedAction = actionsTable.getSelectionModel().getSelectedItem();
        if (selectedAction != null) {
            actionTypeComboBox.setValue(selectedAction.getActionType());
            keysField.setText(selectedAction.getKeys());
            capturedKeys.clear();
            capturedKeys.addAll(List.of(selectedAction.getKeys().split(",")));
            editingAction = selectedAction;
            editActionButton.setText("Сохранить");
        } else if (botController != null) {
            botController.log("Выберите действие для редактирования");
        }
    }

    @FXML
    private void deleteAction() {
        Action selectedAction = actionsTable.getSelectionModel().getSelectedItem();
        if (selectedAction != null) {
            actions.remove(selectedAction);
            clearActionFields();
        } else if (botController != null) {
            botController.log("Выберите действие для удаления");
        }
    }

    private void clearActionFields() {
        keysField.setText("");
        capturedKeys.clear();
        actionTypeComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    private void startBot() {
        try {
            double hpPercent = Double.parseDouble(hpPercentField.getText());
            double mpPercent = Double.parseDouble(mpPercentField.getText());
            String arduinoPort = arduinoPortComboBox.getSelectionModel().getSelectedItem();
            String selectedCharacter = characterComboBox.getSelectionModel().getSelectedItem();
            if (arduinoPort == null || selectedCharacter == null) {
                if (botController != null) {
                    botController.log("Ошибка: порт или персонаж не выбраны");
                }
                return;
            }
            if (hpPercent < 0 || hpPercent > 100 || mpPercent < 0 || mpPercent > 100) {
                if (botController != null) {
                    botController.log("Ошибка: проценты должны быть от 0 до 100");
                }
                return;
            }
            int[] hpBar = parseCoordinates(hpBarField.getText());
            int[] mpBar = parseCoordinates(mpBarField.getText());
            int[] mobHpBar = parseCoordinates(mobHpBarField.getText());
            if (hpBar == null || mpBar == null || mobHpBar == null) {
                if (botController != null) {
                    botController.log("Ошибка: неверный формат координат полос");
                }
                return;
            }
            botController = new BotController(arduinoPort, hpPercent, mpPercent, selectedCharacter, actions, hpBar, mpBar, mobHpBar);
            botController.startBot();
            logArea.textProperty().bind(botController.logProperty());
            startButton.setDisable(true);
            stopButton.setDisable(false);
            startHpMpUpdate(selectedCharacter, hpBar, mpBar);
            // Переключение на окно игры после старта
            activateWindow();
        } catch (NumberFormatException e) {
            if (botController != null) {
                botController.log("Ошибка: неверный формат процентов HP/MP");
            }
        } catch (Exception e) {
            if (botController != null) {
                botController.log("Ошибка запуска: " + e.getMessage());
            }
        }
    }

    @FXML
    private void stopBot() {
        if (botController != null) {
            botController.stopBot();
            startButton.setDisable(false);
            stopButton.setDisable(true);
            logArea.textProperty().unbind();
        }
        if (hpMpUpdateThread != null) {
            hpMpUpdateThread.interrupt();
        }
    }

    private void startHpMpUpdate(String characterWindow, int[] hpBar, int[] mpBar) {
        if (hpMpUpdateThread != null) {
            hpMpUpdateThread.interrupt();
        }
        hpMpUpdateThread = new Thread(() -> {
            ScreenReader screenReader = new ScreenReader();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    double hpLevel = screenReader.readBarLevel(hpBar[0], hpBar[1], hpBar[2], hpBar[3]);
                    double mpLevel = screenReader.readBarLevel(mpBar[0], mpBar[1], mpBar[2], mpBar[3]);
                    Platform.runLater(() -> {
                        hpDisplayField.setText(hpLevel >= 0 ? String.format("%.1f%%", hpLevel * 100) : "Ошибка");
                        mpDisplayField.setText(mpLevel >= 0 ? String.format("%.1f%%", mpLevel * 100) : "Ошибка");
                    });
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (botController != null) {
                        botController.log("Ошибка чтения полос HP/MP: " + e.getMessage());
                    }
                }
            }
        });
        hpMpUpdateThread.setDaemon(true);
        hpMpUpdateThread.start();
    }

    private int[] parseCoordinates(String input) {
        try {
            String[] parts = input.split(",");
            if (parts.length == 4) {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int width = Integer.parseInt(parts[2].trim());
                int height = Integer.parseInt(parts[3].trim());
                if (width <= 0) width = 1; // Минимальная ширина
                if (height <= 0) height = 1; // Минимальная высота
                return new int[]{x, y, width, height};
            }
        } catch (Exception e) {
            if (botController != null) {
                botController.log("Ошибка парсинга координат: " + e.getMessage());
            }
        }
        return null;
    }

    @FXML
    private void selectMobHpBar() {
        if (botController != null) {
            botController.log("Нажмите левую кнопку мыши на начало полосы HP моба в игре, затем правую для завершения. Выделение будет отображаться с лупой.");
        }
        if (mobHpBarField == null) {
            if (botController != null) {
                botController.log("Ошибка: поле mobHpBarField не инициализировано. Проверьте BotUI.fxml.");
            }
            return;
        }
        activateWindow();
        javafx.stage.Popup popup = new javafx.stage.Popup();
        javafx.scene.layout.Pane pane = new javafx.scene.layout.Pane();
        pane.setStyle("-fx-background-color: rgba(0, 0, 255, 0.1);");
        popup.getContent().add(pane);

        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        pane.setPrefSize(screenBounds.getWidth(), screenBounds.getHeight());
        popup.setX(screenBounds.getMinX());
        popup.setY(screenBounds.getMinY());

        try {
            Robot robot = new Robot();
            AtomicReference<Point> startPoint = new AtomicReference<>(null);
            AtomicReference<Point> endPoint = new AtomicReference<>(null);
            AtomicBoolean selecting = new AtomicBoolean(true);

            javafx.stage.Stage zoomStage = new javafx.stage.Stage();
            javafx.scene.image.ImageView zoomView = new javafx.scene.image.ImageView();
            zoomView.setFitWidth(300);
            zoomView.setFitHeight(300);
            javafx.scene.Scene zoomScene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(zoomView));
            zoomStage.setScene(zoomScene);
            zoomStage.setTitle("Лупа");
            zoomStage.initOwner(primaryStage);

            javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseHandler = event -> {
                if (!selecting.get()) return;

                Point currentPoint = java.awt.MouseInfo.getPointerInfo().getLocation();
                if (event.getButton() == MouseButton.PRIMARY && startPoint.get() == null) {
                    startPoint.set(currentPoint);
                    if (botController != null) {
                        botController.log("Начало полосы выбрано: " + currentPoint.x + "," + currentPoint.y);
                    }
                } else if (event.getButton() == MouseButton.SECONDARY && startPoint.get() != null) {
                    endPoint.set(currentPoint);
                    selecting.set(false);
                    popup.hide();
                    zoomStage.close();

                    Point sp = startPoint.get();
                    Point ep = endPoint.get();
                    int x = Math.min(sp.x, ep.x);
                    int y = Math.min(sp.y, ep.y);
                    int width = Math.abs(ep.x - sp.x);
                    int height = Math.abs(ep.y - sp.y);
                    if (width <= 0) width = 1;
                    if (height <= 0) height = 1;
                    mobHpBarField.setText(x + "," + y + "," + width + "," + height);
                    if (botController != null) {
                        botController.log("Полоса HP моба выбрана: " + x + "," + y + "," + width + "," + height);
                    }
                    Platform.runLater(() -> primaryStage.requestFocus());
                }
                pane.requestLayout();
            };

            pane.setOnMousePressed(mouseHandler);
            pane.setOnMouseReleased(mouseHandler);
            pane.setOnMouseDragged(event -> {
                if (selecting.get() && startPoint.get() != null) {
                    endPoint.set(java.awt.MouseInfo.getPointerInfo().getLocation());
                    Point sp = startPoint.get();
                    Point ep = endPoint.get();
                    int x = Math.min(sp.x, ep.x) - 50;
                    int y = Math.min(sp.y, ep.y) - 50;
                    int width = Math.abs(ep.x - sp.x) + 100;
                    int height = Math.abs(ep.y - sp.y) + 100;
                    BufferedImage zoomedImage = robot.createScreenCapture(new java.awt.Rectangle(x, y, width, height));
                    Image fxImage = SwingFXUtils.toFXImage(zoomedImage, null);
                    zoomView.setImage(fxImage);
                    if (!zoomStage.isShowing()) zoomStage.show();
                    pane.requestLayout();
                }
            });

            pane.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
                javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle();
                rect.setFill(javafx.scene.paint.Color.TRANSPARENT);
                rect.setStroke(javafx.scene.paint.Color.RED);
                rect.setStrokeWidth(2);
                Point sp = startPoint.get();
                Point ep = endPoint.get();
                if (sp != null && ep != null) {
                    rect.setX(sp.getX() - screenBounds.getMinX());
                    rect.setY(sp.getY() - screenBounds.getMinY());
                    rect.setWidth(ep.getX() - sp.getX());
                    rect.setHeight(ep.getY() - sp.getY());
                } else if (sp != null) {
                    ep = java.awt.MouseInfo.getPointerInfo().getLocation();
                    rect.setX(sp.getX() - screenBounds.getMinX());
                    rect.setY(sp.getY() - screenBounds.getMinY());
                    rect.setWidth(ep.getX() - sp.getX());
                    rect.setHeight(ep.getY() - sp.getY());
                }
                pane.getChildren().clear();
                pane.getChildren().add(rect);
            });

            popup.show((Stage) characterComboBox.getScene().getWindow());
        } catch (AWTException e) {
            if (botController != null) {
                botController.log("Ошибка инициализации Robot: " + e.getMessage());
            }
        }
    }

    @FXML
    private void selectHpBar() {
        if (botController != null) {
            botController.log("Нажмите левую кнопку мыши на начало полосы HP персонажа в игре, затем правую для завершения. Выделение будет отображаться с лупой.");
        }
        if (hpBarField == null) {
            if (botController != null) {
                botController.log("Ошибка: поле hpBarField не инициализировано. Проверьте BotUI.fxml.");
            }
            return;
        }
        activateWindow();
        javafx.stage.Popup popup = new javafx.stage.Popup();
        javafx.scene.layout.Pane pane = new javafx.scene.layout.Pane();
        pane.setStyle("-fx-background-color: rgba(0, 0, 255, 0.1);");
        popup.getContent().add(pane);

        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        pane.setPrefSize(screenBounds.getWidth(), screenBounds.getHeight());
        popup.setX(screenBounds.getMinX());
        popup.setY(screenBounds.getMinY());

        try {
            Robot robot = new Robot();
            AtomicReference<Point> startPoint = new AtomicReference<>(null);
            AtomicReference<Point> endPoint = new AtomicReference<>(null);
            AtomicBoolean selecting = new AtomicBoolean(true);

            javafx.stage.Stage zoomStage = new javafx.stage.Stage();
            javafx.scene.image.ImageView zoomView = new javafx.scene.image.ImageView();
            zoomView.setFitWidth(300);
            zoomView.setFitHeight(300);
            javafx.scene.Scene zoomScene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(zoomView));
            zoomStage.setScene(zoomScene);
            zoomStage.setTitle("Лупа");
            zoomStage.initOwner(primaryStage);

            javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseHandler = event -> {
                if (!selecting.get()) return;

                Point currentPoint = java.awt.MouseInfo.getPointerInfo().getLocation();
                if (event.getButton() == MouseButton.PRIMARY && startPoint.get() == null) {
                    startPoint.set(currentPoint);
                    if (botController != null) {
                        botController.log("Начало полосы HP выбрано: " + currentPoint.x + "," + currentPoint.y);
                    }
                } else if (event.getButton() == MouseButton.SECONDARY && startPoint.get() != null) {
                    endPoint.set(currentPoint);
                    selecting.set(false);
                    popup.hide();
                    zoomStage.close();

                    Point sp = startPoint.get();
                    Point ep = endPoint.get();
                    int x = Math.min(sp.x, ep.x);
                    int y = Math.min(sp.y, ep.y);
                    int width = Math.abs(ep.x - sp.x);
                    int height = Math.abs(ep.y - sp.y);
                    if (width <= 0) width = 1;
                    if (height <= 0) height = 1;
                    hpBarField.setText(x + "," + y + "," + width + "," + height);
                    if (botController != null) {
                        botController.log("Полоса HP персонажа выбрана: " + x + "," + y + "," + width + "," + height);
                    }
                    Platform.runLater(() -> primaryStage.requestFocus());
                }
                pane.requestLayout();
            };

            pane.setOnMousePressed(mouseHandler);
            pane.setOnMouseReleased(mouseHandler);
            pane.setOnMouseDragged(event -> {
                if (selecting.get() && startPoint.get() != null) {
                    endPoint.set(java.awt.MouseInfo.getPointerInfo().getLocation());
                    Point sp = startPoint.get();
                    Point ep = endPoint.get();
                    int x = Math.min(sp.x, ep.x) - 50;
                    int y = Math.min(sp.y, ep.y) - 50;
                    int width = Math.abs(ep.x - sp.x) + 100;
                    int height = Math.abs(ep.y - sp.y) + 100;
                    BufferedImage zoomedImage = robot.createScreenCapture(new java.awt.Rectangle(x, y, width, height));
                    Image fxImage = SwingFXUtils.toFXImage(zoomedImage, null);
                    zoomView.setImage(fxImage);
                    if (!zoomStage.isShowing()) zoomStage.show();
                    pane.requestLayout();
                }
            });

            pane.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
                javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle();
                rect.setFill(javafx.scene.paint.Color.TRANSPARENT);
                rect.setStroke(javafx.scene.paint.Color.RED);
                rect.setStrokeWidth(2);
                Point sp = startPoint.get();
                Point ep = endPoint.get();
                if (sp != null && ep != null) {
                    rect.setX(sp.getX() - screenBounds.getMinX());
                    rect.setY(sp.getY() - screenBounds.getMinY());
                    rect.setWidth(ep.getX() - sp.getX());
                    rect.setHeight(ep.getY() - sp.getY());
                } else if (sp != null) {
                    ep = java.awt.MouseInfo.getPointerInfo().getLocation();
                    rect.setX(sp.getX() - screenBounds.getMinX());
                    rect.setY(sp.getY() - screenBounds.getMinY());
                    rect.setWidth(ep.getX() - sp.getX());
                    rect.setHeight(ep.getY() - sp.getY());
                }
                pane.getChildren().clear();
                pane.getChildren().add(rect);
            });

            popup.show((Stage) characterComboBox.getScene().getWindow());
        } catch (AWTException e) {
            if (botController != null) {
                botController.log("Ошибка инициализации Robot: " + e.getMessage());
            }
        }
    }

    @FXML
    private void selectMpBar() {
        if (botController != null) {
            botController.log("Нажмите левую кнопку мыши на начало полосы MP персонажа в игре, затем правую для завершения. Выделение будет отображаться с лупой.");
        }
        if (mpBarField == null) {
            if (botController != null) {
                botController.log("Ошибка: поле mpBarField не инициализировано. Проверьте BotUI.fxml.");
            }
            return;
        }
        activateWindow();
        javafx.stage.Popup popup = new javafx.stage.Popup();
        javafx.scene.layout.Pane pane = new javafx.scene.layout.Pane();
        pane.setStyle("-fx-background-color: rgba(0, 0, 255, 0.1);");
        popup.getContent().add(pane);

        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        pane.setPrefSize(screenBounds.getWidth(), screenBounds.getHeight());
        popup.setX(screenBounds.getMinX());
        popup.setY(screenBounds.getMinY());

        try {
            Robot robot = new Robot();
            AtomicReference<Point> startPoint = new AtomicReference<>(null);
            AtomicReference<Point> endPoint = new AtomicReference<>(null);
            AtomicBoolean selecting = new AtomicBoolean(true);

            javafx.stage.Stage zoomStage = new javafx.stage.Stage();
            javafx.scene.image.ImageView zoomView = new javafx.scene.image.ImageView();
            zoomView.setFitWidth(300);
            zoomView.setFitHeight(300);
            javafx.scene.Scene zoomScene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(zoomView));
            zoomStage.setScene(zoomScene);
            zoomStage.setTitle("Лупа");
            zoomStage.initOwner(primaryStage);

            javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseHandler = event -> {
                if (!selecting.get()) return;

                Point currentPoint = java.awt.MouseInfo.getPointerInfo().getLocation();
                if (event.getButton() == MouseButton.PRIMARY && startPoint.get() == null) {
                    startPoint.set(currentPoint);
                    if (botController != null) {
                        botController.log("Начало полосы MP выбрано: " + currentPoint.x + "," + currentPoint.y);
                    }
                } else if (event.getButton() == MouseButton.SECONDARY && startPoint.get() != null) {
                    endPoint.set(currentPoint);
                    selecting.set(false);
                    popup.hide();
                    zoomStage.close();

                    Point sp = startPoint.get();
                    Point ep = endPoint.get();
                    int x = Math.min(sp.x, ep.x);
                    int y = Math.min(sp.y, ep.y);
                    int width = Math.abs(ep.x - sp.x);
                    int height = Math.abs(ep.y - sp.y);
                    if (width <= 0) width = 1;
                    if (height <= 0) height = 1;
                    mpBarField.setText(x + "," + y + "," + width + "," + height);
                    if (botController != null) {
                        botController.log("Полоса MP персонажа выбрана: " + x + "," + y + "," + width + "," + height);
                    }
                    Platform.runLater(() -> primaryStage.requestFocus());
                }
                pane.requestLayout();
            };

            pane.setOnMousePressed(mouseHandler);
            pane.setOnMouseReleased(mouseHandler);
            pane.setOnMouseDragged(event -> {
                if (selecting.get() && startPoint.get() != null) {
                    endPoint.set(java.awt.MouseInfo.getPointerInfo().getLocation());
                    Point sp = startPoint.get();
                    Point ep = endPoint.get();
                    int x = Math.min(sp.x, ep.x) - 50;
                    int y = Math.min(sp.y, ep.y) - 50;
                    int width = Math.abs(ep.x - sp.x) + 100;
                    int height = Math.abs(ep.y - sp.y) + 100;
                    BufferedImage zoomedImage = robot.createScreenCapture(new java.awt.Rectangle(x, y, width, height));
                    Image fxImage = SwingFXUtils.toFXImage(zoomedImage, null);
                    zoomView.setImage(fxImage);
                    if (!zoomStage.isShowing()) zoomStage.show();
                    pane.requestLayout();
                }
            });

            pane.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
                javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle();
                rect.setFill(javafx.scene.paint.Color.TRANSPARENT);
                rect.setStroke(javafx.scene.paint.Color.RED);
                rect.setStrokeWidth(2);
                Point sp = startPoint.get();
                Point ep = endPoint.get();
                if (sp != null && ep != null) {
                    rect.setX(sp.getX() - screenBounds.getMinX());
                    rect.setY(sp.getY() - screenBounds.getMinY());
                    rect.setWidth(ep.getX() - sp.getX());
                    rect.setHeight(ep.getY() - sp.getY());
                } else if (sp != null) {
                    ep = java.awt.MouseInfo.getPointerInfo().getLocation();
                    rect.setX(sp.getX() - screenBounds.getMinX());
                    rect.setY(sp.getY() - screenBounds.getMinY());
                    rect.setWidth(ep.getX() - sp.getX());
                    rect.setHeight(ep.getY() - sp.getY());
                }
                pane.getChildren().clear();
                pane.getChildren().add(rect);
            });

            popup.show((Stage) characterComboBox.getScene().getWindow());
        } catch (AWTException e) {
            if (botController != null) {
                botController.log("Ошибка инициализации Robot: " + e.getMessage());
            }
        }
    }

    public ObservableList<Action> getActions() {
        return actions;
    }

    @FXML
    private void saveSettings() {
        JSONObject settings = new JSONObject();
        settings.put("hpPercent", hpPercentField.getText());
        settings.put("mpPercent", mpPercentField.getText());
        settings.put("hpBar", hpBarField.getText());
        settings.put("mpBar", mpBarField.getText());
        settings.put("mobHpBar", mobHpBarField.getText());
        settings.put("arduinoPort", arduinoPortComboBox.getSelectionModel().getSelectedItem());
        settings.put("character", characterComboBox.getSelectionModel().getSelectedItem());
        JSONObject actionsJson = new JSONObject();
        for (Action action : actions) {
            actionsJson.put(action.getActionType(), action.getKeys());
        }
        settings.put("actions", actionsJson);

        try (FileWriter file = new FileWriter("settings.json")) {
            file.write(settings.toString(4));
            if (botController != null) {
                botController.log("Настройки сохранены в settings.json");
            }
        } catch (IOException e) {
            if (botController != null) {
                botController.log("Ошибка сохранения настроек: " + e.getMessage());
            }
        }
    }

    @FXML
    private void loadSettings() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("settings.json")));
            JSONObject settings = new JSONObject(content);

            hpPercentField.setText(settings.getString("hpPercent"));
            mpPercentField.setText(settings.getString("mpPercent"));
            hpBarField.setText(settings.getString("hpBar"));
            mpBarField.setText(settings.getString("mpBar"));
            mobHpBarField.setText(settings.getString("mobHpBar"));
            arduinoPortComboBox.getSelectionModel().select(settings.getString("arduinoPort"));
            characterComboBox.getSelectionModel().select(settings.getString("character"));

            actions.clear();
            JSONObject actionsJson = settings.getJSONObject("actions");
            for (String actionType : actionTypeComboBox.getItems()) {
                if (actionsJson.has(actionType)) {
                    actions.add(new Action(actionType, actionsJson.getString(actionType)));
                }
            }

            if (botController != null) {
                botController.log("Настройки загружены из settings.json");
            }
        } catch (IOException e) {
            if (botController != null) {
                botController.log("Ошибка загрузки настроек: файл settings.json не найден");
            }
        }
    }
}
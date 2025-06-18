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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.util.StringConverter;
import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BotUIController {
    @FXML private TextField characterNameField;
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
    @FXML private ComboBox<String> skillComboBox;
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
    @FXML private ComboBox<ClassId> classComboBox;

    private static final int MAX_LOG_LINES = 100;
    private static final long LOG_UPDATE_DELAY_MS = 200;
    private final StringBuilder logBuffer = new StringBuilder();
    private volatile boolean isRunning = false;
    private long lastLogUpdateTime = 0;
    private final Object logSync = new Object();
    private BotController botController;
    private Thread hpMpUpdateThread;
    private final ObservableList<Action> actions = FXCollections.observableArrayList();
    private final ObservableList<String> availableSkills = FXCollections.observableArrayList();
    private List<String> capturedKeys = new ArrayList<>();
    private Action editingAction;
    private Stage primaryStage;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    private void initialize() {
        stopButton.setDisable(true);
        actionTypeColumn.setCellValueFactory(cellData -> cellData.getValue().actionTypeProperty());
        keysColumn.setCellValueFactory(cellData -> cellData.getValue().keysProperty());
        actionsTable.setItems(actions);

        hpPercentField.setText("30");
        mpPercentField.setText("30");
        hpBarField.setText("50,50,100,10");
        mpBarField.setText("50,80,100,10");
        mobHpBarField.setText("200,100,50,10");

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

        validatePercentField(hpPercentField);
        validatePercentField(mpPercentField);
        limitLogArea();
        setupHotKeys();

        characterComboBox.setVisible(true);
        refreshCharactersButton.setVisible(true);
        characterComboBox.setStyle("-fx-font-size: 12px; -fx-pref-width: 250px;");

        classComboBox.getItems().addAll(ClassId.getPlayableClasses());
        classComboBox.setConverter(new StringConverter<ClassId>() {
            @Override
            public String toString(ClassId classId) {
                return classId != null ? classId.getDisplayName() : "";
            }
            @Override
            public ClassId fromString(String string) {
                return Arrays.stream(ClassId.values())
                        .filter(c -> c.getDisplayName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
        classComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadSkillsForClass(newVal);
                log("Выбран класс: " + newVal.getDisplayName());
            }
        });

        skillComboBox.setItems(availableSkills);
    }

    private void loadSkillsForClass(ClassId classId) {
        availableSkills.clear();
        List<Skill> classSkills = SkillList.getSkillsForClass(classId);
        availableSkills.addAll(classSkills.stream().map(Skill::getName).collect(Collectors.toList()));
        availableSkills.addAll(Arrays.asList("Auto Attack", "Next Target", "Low HP", "Low MP"));
        log("Загружено " + availableSkills.size() + " скиллов и действий для класса " + classId.getDisplayName());
    }

    private void setupHotKeys() {
        // Пустая реализация
    }

    private void validatePercentField(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                try {
                    double value = Double.parseDouble(newVal);
                    if (value < 0 || value > 100) {
                        Platform.runLater(() -> field.setText(oldVal));
                        log("Ошибка: процент должен быть от 0 до 100");
                    }
                } catch (NumberFormatException e) {
                    Platform.runLater(() -> field.setText(oldVal));
                    log("Ошибка: введите число");
                }
            }
        });
    }

    private void limitLogArea() {
        logArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                Platform.runLater(() -> {
                    logBuffer.append(newVal).append("\n");
                    String[] lines = logBuffer.toString().split("\n");
                    if (lines.length > MAX_LOG_LINES) {
                        logBuffer.setLength(0);
                        for (int i = lines.length - MAX_LOG_LINES; i < lines.length; i++) {
                            logBuffer.append(lines[i]).append("\n");
                        }
                    }
                    logArea.setText(logBuffer.toString());
                    logArea.setScrollTop(Double.MAX_VALUE);
                });
            }
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
    private void detectGameWindow() {
        String nickname = characterNameField.getText().trim();
        if (nickname.isEmpty()) {
            log("❌ Введите ник персонажа!");
            return;
        }

        List<String> gameWindows = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hWnd, arg) -> {
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hWnd, windowText, 512);
            String title = new String(windowText).trim();

            if (title.contains(nickname)) {
                char[] className = new char[256];
                User32.INSTANCE.GetClassName(hWnd, className, 256);
                String windowClass = new String(className).trim();

                if (windowClass.equals("UnrealWindow") ||
                        windowClass.equals("Lineage") ||
                        title.matches(nickname + ".*")) {
                    gameWindows.add(title);
                }
            }
            return true;
        }, null);

        if (!gameWindows.isEmpty()) {
            characterComboBox.getItems().setAll(gameWindows);
            characterComboBox.getSelectionModel().selectFirst();
            log("✅ Окно игры найдено: " + gameWindows.get(0));
            activateWindow();
        } else {
            log("❌ Не найдено окно с ником '" + nickname + "'");
            characterComboBox.setVisible(true);
            refreshCharactersButton.setVisible(true);
            refreshCharacters();
        }
    }

    private boolean isGameWindow(WinDef.HWND hWnd, String title) {
        char[] className = new char[256];
        User32.INSTANCE.GetClassName(hWnd, className, 256);
        String windowClass = new String(className).trim();

        return windowClass.equals("UnrealWindow") ||
                windowClass.equals("Lineage") ||
                (windowClass.startsWith("WindowsForms") && title.matches(".*[A-Za-z0-9_]{3,16}.*"));
    }

    @FXML
    private void refreshCharacters() {
        characterComboBox.getItems().clear();
        User32.INSTANCE.EnumWindows((hWnd, arg) -> {
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hWnd, windowText, 512);
            String title = new String(windowText).trim();

            if (!title.isEmpty() && isGameWindow(hWnd, title)) {
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
            } else {
                log("Окна Lineage 2 не найдены. Убедитесь, что игра запущена.");
            }
        });
    }

    @FXML
    private void activateWindow() {
        String selectedWindow = characterComboBox.getSelectionModel().getSelectedItem();
        if (selectedWindow != null) {
            WinDef.HWND hWnd = User32.INSTANCE.FindWindow(null, selectedWindow);
            if (hWnd != null) {
                User32.INSTANCE.SetForegroundWindow(hWnd);
                log("Активировано окно: " + selectedWindow);
            } else {
                log("Ошибка: окно '" + selectedWindow + "' не найдено");
            }
        } else {
            log("Ошибка: окно не выбрано");
        }
    }

    @FXML
    private void addAction() {
        String actionType = skillComboBox.getSelectionModel().getSelectedItem();
        String keys = keysField.getText().trim();
        if (actionType == null || keys.isEmpty()) {
            log("Ошибка: скилл/действие или клавиши не выбраны");
            return;
        }

        for (Action existingAction : actions) {
            if (existingAction.getActionType().equals(actionType)) {
                log("Ошибка: скилл/действие '" + actionType + "' уже существует");
                return;
            }
            if (editingAction != existingAction && existingAction.getKeys().equals(keys)) {
                log("Ошибка: клавиши '" + keys + "' уже используются");
                return;
            }
        }

        if (editingAction != null) {
            editingAction.setActionType(actionType);
            editingAction.setKeys(keys);
            editingAction = null;
            editActionButton.setText("Редактировать");
        } else {
            actions.add(new Action(actionType, keys));
        }
        clearActionFields();
        log("Скилл/действие добавлено/обновлено: " + actionType);
    }

    @FXML
    private void editAction() {
        Action selectedAction = actionsTable.getSelectionModel().getSelectedItem();
        if (selectedAction != null) {
            skillComboBox.setValue(selectedAction.getActionType());
            keysField.setText(selectedAction.getKeys());
            capturedKeys.clear();
            capturedKeys.addAll(List.of(selectedAction.getKeys().split(",")));
            editingAction = selectedAction;
            editActionButton.setText("Сохранить");
        } else {
            log("Выберите скилл/действие для редактирования");
        }
    }

    @FXML
    private void deleteAction() {
        Action selectedAction = actionsTable.getSelectionModel().getSelectedItem();
        if (selectedAction != null) {
            actions.remove(selectedAction);
            clearActionFields();
            log("Скилл/действие удалено");
        } else {
            log("Выберите скилл/действие для удаления");
        }
    }

    private void clearActionFields() {
        keysField.setText("");
        capturedKeys.clear();
        skillComboBox.getSelectionModel().clearSelection();
        editingAction = null;
        editActionButton.setText("Редактировать");
    }

    @FXML
    private void startBot() {
        if (isRunning) return;
        isRunning = true;

        try {
            synchronized (logBuffer) {
                logBuffer.setLength(0);
                Platform.runLater(() -> logArea.setText(""));
            }

            double hpPercent = Double.parseDouble(hpPercentField.getText());
            double mpPercent = Double.parseDouble(mpPercentField.getText());
            String arduinoPort = arduinoPortComboBox.getSelectionModel().getSelectedItem();
            String selectedCharacter = characterComboBox.getSelectionModel().getSelectedItem();

            if (arduinoPort == null || selectedCharacter == null) {
                log("Ошибка: порт или персонаж не выбраны");
                isRunning = false;
                return;
            }

            int[] hpBar = parseCoordinates(hpBarField.getText(), "HP персонажа");
            int[] mpBar = parseCoordinates(mpBarField.getText(), "MP персонажа");
            int[] mobHpBar = parseCoordinates(mobHpBarField.getText(), "HP моба");

            if (hpBar == null || mpBar == null || mobHpBar == null) {
                log("Ошибка: неверный формат координат полос");
                isRunning = false;
                return;
            }

            botController = new BotController(arduinoPort, hpPercent, mpPercent,
                    selectedCharacter, actions, FXCollections.observableArrayList(), hpBar, mpBar, mobHpBar);

            botController.logProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isEmpty()) {
                    synchronized (logBuffer) {
                        logBuffer.append(newVal).append("\n");
                    }
                    scheduleLogUpdate();
                }
            });

            botController.startBot();
            startButton.setDisable(true);
            stopButton.setDisable(false);
            startHpMpUpdate(selectedCharacter, hpBar, mpBar);
            activateWindow();

        } catch (NumberFormatException e) {
            log("Ошибка: неверный формат процентов HP/MP");
            isRunning = false;
        } catch (Exception e) {
            log("Ошибка запуска: " + e.getMessage());
            isRunning = false;
        }
    }

    @FXML
    private void stopBot() {
        if (!isRunning) return;
        isRunning = false;

        if (botController != null) {
            botController.stopBot();
        }

        if (hpMpUpdateThread != null) {
            hpMpUpdateThread.interrupt();
            hpMpUpdateThread = null;
        }

        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

    private void scheduleLogUpdate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogUpdateTime > LOG_UPDATE_DELAY_MS) {
            lastLogUpdateTime = currentTime;
            Platform.runLater(this::updateLogDisplay);
        }
    }

    private void updateLogDisplay() {
        if (!isRunning) return;

        synchronized (logBuffer) {
            String currentText = logArea.getText();
            String[] bufferLines = logBuffer.toString().split("\n");

            if (bufferLines.length > MAX_LOG_LINES) {
                int startIdx = bufferLines.length - MAX_LOG_LINES;
                StringBuilder trimmed = new StringBuilder();
                for (int i = startIdx; i < bufferLines.length; i++) {
                    trimmed.append(bufferLines[i]).append("\n");
                }
                logBuffer.setLength(0);
                logBuffer.append(trimmed);
                currentText = trimmed.toString();
            } else {
                currentText = logBuffer.toString();
            }

            logArea.setText(currentText);
            logArea.setScrollTop(Double.MAX_VALUE);
        }
    }

    private void startHpMpUpdate(String characterWindow, int[] hpBar, int[] mpBar) {
        if (hpMpUpdateThread != null) {
            hpMpUpdateThread.interrupt();
            try {
                hpMpUpdateThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        hpMpUpdateThread = new Thread(() -> {
            ScreenReader screenReader = new ScreenReader();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    double hpLevel = screenReader.readBarLevel(hpBar[0], hpBar[1], hpBar[2], hpBar[3]);
                    double mpLevel = screenReader.readBarLevel(mpBar[0], mpBar[1], mpBar[2], mpBar[3]);

                    Platform.runLater(() -> {
                        hpDisplayField.setText(String.format("%.1f%%", hpLevel * 100));
                        mpDisplayField.setText(String.format("%.1f%%", mpLevel * 100));
                    });
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log("Ошибка чтения полос HP/MP: " + e.getMessage());
                }
            }
        });
        hpMpUpdateThread.setDaemon(true);
        hpMpUpdateThread.start();
    }

    private int[] parseCoordinates(String input, String fieldName) {
        try {
            String[] parts = input.split(",");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Неверный формат координат: ожидается x,y,width,height");
            }
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int width = Integer.parseInt(parts[2].trim());
            int height = Integer.parseInt(parts[3].trim());

            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Ширина и высота должны быть положительными");
            }

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (x < 0 || y < 0 || x + width > screenSize.width || y + height > screenSize.height) {
                throw new IllegalArgumentException("Координаты выходят за пределы экрана");
            }

            return new int[]{x, y, width, height};
        } catch (Exception e) {
            log("Ошибка парсинга координат для " + fieldName + ": " + e.getMessage());
            return null;
        }
    }

    private void selectBar(TextField targetField, String barName) {
        log("Нажмите левую кнопку мыши на начало полосы " + barName + ", затем правую для завершения.");

        Popup popup = new Popup();
        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: rgba(0, 0, 255, 0.1);");
        popup.getContent().add(pane);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        pane.setPrefSize(screenBounds.getWidth(), screenBounds.getHeight());
        popup.setX(screenBounds.getMinX());
        popup.setY(screenBounds.getMinY());

        try {
            Robot robot = new Robot();
            AtomicReference<Point> startPoint = new AtomicReference<>(null);
            AtomicReference<Point> endPoint = new AtomicReference<>(null);
            AtomicBoolean selecting = new AtomicBoolean(true);

            Stage zoomStage = new Stage();
            ImageView zoomView = new ImageView();
            zoomView.setFitWidth(300);
            zoomView.setFitHeight(300);
            Scene zoomScene = new Scene(new StackPane(zoomView));
            zoomStage.setScene(zoomScene);
            zoomStage.setTitle("Лупа");
            zoomStage.initOwner(primaryStage);

            javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseHandler = event -> {
                if (!selecting.get()) return;

                Point currentPoint = MouseInfo.getPointerInfo().getLocation();
                if (event.getButton() == MouseButton.PRIMARY && startPoint.get() == null) {
                    startPoint.set(currentPoint);
                    log("Начало полосы " + barName + " выбрано: " + currentPoint.x + "," + currentPoint.y);
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
                    targetField.setText(x + "," + y + "," + width + "," + height);
                    log("Полоса " + barName + " выбрана: " + x + "," + y + "," + width + "," + height);
                    Platform.runLater(() -> primaryStage.requestFocus());
                }
                pane.requestLayout();
            };

            pane.setOnMousePressed(mouseHandler);
            pane.setOnMouseReleased(mouseHandler);
            pane.setOnMouseDragged(event -> {
                if (selecting.get() && startPoint.get() != null) {
                    endPoint.set(MouseInfo.getPointerInfo().getLocation());
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

            popup.show(primaryStage);
        } catch (AWTException e) {
            log("Ошибка инициализации Robot: " + e.getMessage());
        }
    }

    @FXML
    private void selectMobHpBar() { selectBar(mobHpBarField, "HP моба"); }
    @FXML
    private void selectHpBar() { selectBar(hpBarField, "HP персонажа"); }
    @FXML
    private void selectMpBar() { selectBar(mpBarField, "MP персонажа"); }

    @FXML
    private void saveSettings() {
        JSONObject settings = new JSONObject();
        settings.put("characterName", characterNameField.getText());
        settings.put("hpPercent", hpPercentField.getText());
        settings.put("mpPercent", mpPercentField.getText());
        settings.put("hpBar", hpBarField.getText());
        settings.put("mpBar", mpBarField.getText());
        settings.put("mobHpBar", mobHpBarField.getText());
        settings.put("arduinoPort", arduinoPortComboBox.getSelectionModel().getSelectedItem());
        settings.put("character", characterComboBox.getSelectionModel().getSelectedItem());
        settings.put("class", classComboBox.getSelectionModel().getSelectedItem() != null ?
                classComboBox.getSelectionModel().getSelectedItem().getDisplayName() : "");

        JSONObject actionsJson = new JSONObject();
        for (Action action : actions) {
            actionsJson.put(action.getActionType(), action.getKeys());
        }
        settings.put("actions", actionsJson);

        try (FileWriter file = new FileWriter("settings.json")) {
            file.write(settings.toString(4));
            log("Настройки сохранены в settings.json");
        } catch (IOException e) {
            log("Ошибка сохранения настроек: " + e.getMessage());
        }
    }

    @FXML
    private void loadSettings() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("settings.json")));
            JSONObject settings = new JSONObject(content);

            characterNameField.setText(settings.optString("characterName", ""));
            hpPercentField.setText(settings.getString("hpPercent"));
            mpPercentField.setText(settings.getString("mpPercent"));
            hpBarField.setText(settings.getString("hpBar"));
            mpBarField.setText(settings.getString("mpBar"));
            mobHpBarField.setText(settings.getString("mobHpBar"));
            arduinoPortComboBox.getSelectionModel().select(settings.getString("arduinoPort"));

            String character = settings.optString("character", "");
            if (!character.isEmpty()) {
                characterComboBox.getItems().add(character);
                characterComboBox.getSelectionModel().selectFirst();
                activateWindow();
            }

            String className = settings.optString("class", "");
            if (!className.isEmpty()) {
                ClassId selectedClass = Arrays.stream(ClassId.values())
                        .filter(c -> c.getDisplayName().equals(className))
                        .findFirst()
                        .orElse(null);
                if (selectedClass != null) {
                    classComboBox.getSelectionModel().select(selectedClass);
                }
            }

            actions.clear();
            JSONObject actionsJson = settings.getJSONObject("actions");
            for (String key : actionsJson.keySet()) {
                actions.add(new Action(key, actionsJson.getString(key)));
            }

            log("Настройки загружены из settings.json");
        } catch (IOException e) {
            log("Ошибка загрузки настроек: файл settings.json не найден");
        }
    }

    private void log(String message) {
        synchronized (logBuffer) {
            logBuffer.append(message).append("\n");
            scheduleLogUpdate();
        }
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
}
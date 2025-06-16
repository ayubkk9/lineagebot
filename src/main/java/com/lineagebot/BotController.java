package com.lineagebot;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class BotController {
    private final ScreenReader screenReader;
    private final ArduinoInterface arduino;
    private volatile boolean running = false;
    private final double hpPercent;
    private final double mpPercent;
    private final StringProperty log = new SimpleStringProperty();
    private final String characterWindow;
    private final ObservableList<BotUIController.Action> actions;
    private final int[] hpBar;
    private final int[] mpBar;
    private final int[] mobHpBar;
    private final Object lock = new Object();

    public BotController(String arduinoPort, double hpPercent, double mpPercent, String characterWindow,
                         ObservableList<BotUIController.Action> actions, int[] hpBar, int[] mpBar, int[] mobHpBar) {
        this.screenReader = new ScreenReader();
        this.arduino = new ArduinoInterface(arduinoPort, this::log);
        this.hpPercent = hpPercent / 100.0;
        this.mpPercent = mpPercent / 100.0;
        this.characterWindow = characterWindow;
        this.actions = actions;
        this.hpBar = hpBar;
        this.mpBar = mpBar;
        this.mobHpBar = mobHpBar;
    }

    public void startBot() throws Exception {
        synchronized (lock) {
            if (!arduino.isPortOpen()) {
                log("Ошибка: порт Arduino не открыт");
                throw new Exception("Порт Arduino не доступен");
            }
        }
        running = true;
        new Thread(() -> {
            while (running) {
                try {
                    if (!isWindowActive(characterWindow)) {
                        log("Окно " + characterWindow + " не активно, пропуск цикла");
                        Thread.sleep(9000);
                        continue;
                    }

                    String searchKeys = getActionKeys("Поиск Моба");
                    if (!searchKeys.isEmpty()) {
                        synchronized (lock) {
                            for (String key : searchKeys.split(",")) {
                                arduino.sendCommand("PRESS_KEY:" + key.trim());
                                log("Поиск цели: нажата " + key.trim());
                                Thread.sleep(200);
                            }
                        }
                    } else {
                        log("Клавиша поиска не задана");
                    }

                    double currentMobHP;
                    synchronized (lock) {
                        currentMobHP = screenReader.readBarLevel(mobHpBar[0], mobHpBar[1], mobHpBar[2], mobHpBar[3]);
                    }
                    log("HP моба: " + String.format("%.1f%%", currentMobHP * 100));
                    while (currentMobHP > 0.01 && running) {
                        String attackKeys = getActionKeys("Атака моба");
                        if (!attackKeys.isEmpty()) {
                            synchronized (lock) {
                                for (String key : attackKeys.split(",")) {
                                    arduino.sendCommand("PRESS_KEY:" + key.trim());
                                    log("Выполнено действие: " + key.trim());
                                    Thread.sleep(200);
                                }
                            }
                        } else {
                            log("Клавиши для атаки моба не заданы");
                            break;
                        }

                        synchronized (lock) {
                            currentMobHP = screenReader.readBarLevel(mobHpBar[0], mobHpBar[1], mobHpBar[2], mobHpBar[3]);
                        }
                        log("HP моба после атаки: " + String.format("%.1f%%", currentMobHP * 100));
                        Thread.sleep(500);
                    }

                    if (currentMobHP <= 0.01 && !getActionKeys("Моб убит").isEmpty()) {
                        log("Моб мёртв, переключение...");
                        String deadKeys = getActionKeys("Моб убит");
                        synchronized (lock) {
                            for (String key : deadKeys.split(",")) {
                                arduino.sendCommand("PRESS_KEY:" + key.trim());
                                log("Выполнено действие: " + key.trim());
                                Thread.sleep(200);
                            }
                        }
                    }

                    String mpKey = getActionKeys("Низкое MP");
                    if (!mpKey.isEmpty()) {
                        double playerMP;
                        synchronized (lock) {
                            playerMP = screenReader.readBarLevel(mpBar[0], mpBar[1], mpBar[2], mpBar[3]);
                        }
                        if (playerMP < mpPercent) {
                            synchronized (lock) {
                                arduino.sendCommand("PRESS_KEY:" + mpKey);
                            }
                            log("Использовано зелье MP: " + mpKey);
                            Thread.sleep(200);
                        }
                    }

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log("Ошибка: " + e.getMessage());
                }
            }
        }).start();
    }

    public void stopBot() {
        running = false;
        synchronized (lock) {
            arduino.close();
        }
        log("Бот остановлен");
    }

    public StringProperty logProperty() {
        return log;
    }

    public void log(String message) {
        Platform.runLater(() -> log.set(log.get() + message + "\n"));
    }

    private String getActionKeys(String actionType) {
        for (BotUIController.Action action : actions) {
            if (action.getActionType().equals(actionType)) {
                return action.getKeys();
            }
        }
        return "";
    }

    private boolean isWindowActive(String windowTitle) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            WinDef.HWND hWnd = User32.INSTANCE.FindWindow(null, windowTitle);
            return hWnd != null && User32.INSTANCE.IsWindowVisible(hWnd);
        }
        // Для macOS и Linux использовать альтернативу, например, через ProcessHandle
        try {
            return ProcessHandle.allProcesses()
                    .map(ProcessHandle::info)
                    .map(ProcessHandle.Info::command)
                    .filter(cmd -> cmd.isPresent() && cmd.get().contains(windowTitle))
                    .findAny()
                    .isPresent();
        } catch (Exception e) {
            log("Ошибка проверки активности окна на " + os + ": " + e.getMessage());
            return true; // Заглушка для других ОС
        }
    }
}
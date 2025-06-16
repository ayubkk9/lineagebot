package com.lineagebot;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.bytedeco.opencv.opencv_core.Rect;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import java.util.List;

public class BotController {
    private ScreenReader screenReader;
    private ArduinoInterface arduino;
    private boolean running = false;
    private double hpPercent;
    private double mpPercent;
    private StringProperty log = new SimpleStringProperty();
    private String characterWindow;
    private ObservableList<BotUIController.Action> actions;
    private int[] hpBar;
    private int[] mpBar;
    private int[] mobHpBar;

    public BotController(String arduinoPort, double hpPercent, double mpPercent, String characterWindow,
                         ObservableList<BotUIController.Action> actions, int[] hpBar, int[] mpBar, int[] mobHpBar) {
        screenReader = new ScreenReader();
        arduino = new ArduinoInterface(arduinoPort);
        this.hpPercent = hpPercent / 100.0;
        this.mpPercent = mpPercent / 100.0;
        this.characterWindow = characterWindow;
        this.actions = actions;
        this.hpBar = hpBar;
        this.mpBar = mpBar;
        this.mobHpBar = mobHpBar;
    }

    public void startBot() throws Exception {
        if (!arduino.isPortOpen()) {
            log("Ошибка: порт Arduino не открыт");
            throw new Exception("Порт Arduino не доступен");
        }
        running = true;
        new Thread(() -> {
            while (running) {
                try {
                    // Проверяем активность окна
                    WinDef.HWND hWnd = User32.INSTANCE.FindWindow(null, characterWindow);
                    if (hWnd == null || !User32.INSTANCE.IsWindowVisible(hWnd)) {
                        log("Окно " + characterWindow + " не активно, пропуск цикла");
                        Thread.sleep(9000);
                        continue;
                    }

                    // 1. Поиск моба
                    String searchKeys = getActionKeys("Поиск Моба");
                    log("Поиск Моба: клавиши = " + searchKeys);
                    if (!searchKeys.isEmpty()) {
                        for (String key : searchKeys.split(",")) {
                            arduino.sendCommand("PRESS_KEY:" + key.trim());
                            log("Поиск цели: нажата " + key.trim());
                            Thread.sleep(200);
                        }
                    } else {
                        log("Клавиша поиска не задана");
                    }

                    // 2. Проверка HP моба и атака текущего моба
                    List<Rect> mobLocations = screenReader.findMobLocations();
                    if (!mobLocations.isEmpty()) {
                        Rect currentMob = mobLocations.get(0);
                        double currentMobHP = screenReader.readBarLevel(mobHpBar[0], mobHpBar[1], mobHpBar[2], mobHpBar[3]);
                        String owner = screenReader.readMobOwner(currentMob.x(), currentMob.y() - 20, currentMob.width(), 20);
                        if (!owner.isEmpty()) {
                            log("Моб атакуется игроком: " + owner + ", пропуск");
                            Thread.sleep(1000);
                            continue;
                        }

                        log("HP моба: " + String.format("%.1f%%", currentMobHP * 100));
                        while (currentMobHP > 0.01) { // Продолжаем атаку, пока моб жив
                            // Проверка и выполнение только заданных действий
                            String attackKeys = getActionKeys("Атака моба");
                            log("Атака моба: клавиши = " + attackKeys);
                            if (!attackKeys.isEmpty()) {
                                for (String key : attackKeys.split(",")) {
                                    arduino.sendCommand("PRESS_KEY:" + key.trim());
                                    log("Выполнено действие: " + key.trim());
                                    Thread.sleep(200);
                                }
                            } else {
                                log("Клавиши для атаки моба не заданы");
                                break;
                            }

                            // Перепроверяем HP моба
                            currentMobHP = screenReader.readBarLevel(mobHpBar[0], mobHpBar[1], mobHpBar[2], mobHpBar[3]);
                            log("HP моба после атаки: " + String.format("%.1f%%", currentMobHP * 100));
                            Thread.sleep(500);
                        }

                        // 3. Моб убит (только если действие определено)
                        if (currentMobHP <= 0.01 && !getActionKeys("Моб убит").isEmpty()) {
                            log("Моб мёртв, переключение...");
                            String deadKeys = getActionKeys("Моб убит");
                            log("Моб убит: клавиши = " + deadKeys);
                            if (!deadKeys.isEmpty()) {
                                for (String key : deadKeys.split(",")) {
                                    arduino.sendCommand("PRESS_KEY:" + key.trim());
                                    log("Выполнено действие: " + key.trim());
                                    Thread.sleep(200);
                                }
                            }
                        }
                    }

                    // 4. Проверка MP (только если действие определено)
                    String mpKey = getActionKeys("Низкое MP");
                    double playerMP = screenReader.readBarLevel(mpBar[0], mpBar[1], mpBar[2], mpBar[3]);
                    if (!mpKey.isEmpty() && playerMP < mpPercent) {
                        log("Низкое MP: клавиши = " + mpKey);
                        arduino.sendCommand("PRESS_KEY:" + mpKey);
                        log("Использовано зелье MP: " + mpKey);
                        Thread.sleep(200);
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
        arduino.close();
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
}
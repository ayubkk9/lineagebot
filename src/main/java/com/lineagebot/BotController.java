package com.lineagebot;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class BotController {
    private final ScreenReader screenReader;
    private final ArduinoInterface arduino;
    private volatile boolean running = false;
    private final double hpPercent;
    private final double mpPercent;
    private final StringProperty log = new SimpleStringProperty("");
    private final String characterWindow;
    private final ObservableList<BotUIController.Action> actions;
    private final ObservableList<Skill> skills;
    private final int[] hpBar;
    private final int[] mpBar;
    private final int[] mobHpBar;
    private final Object lock = new Object();
    private final Random random = new Random();

    public BotController(String arduinoPort, double hpPercent, double mpPercent, String characterWindow,
                         ObservableList<BotUIController.Action> actions, ObservableList<Skill> skills,
                         int[] hpBar, int[] mpBar, int[] mobHpBar) {
        this.screenReader = new ScreenReader();
        this.arduino = new ArduinoInterface(arduinoPort, this::log);
        this.hpPercent = hpPercent / 100.0;
        this.mpPercent = mpPercent / 100.0;
        this.characterWindow = characterWindow;
        this.actions = actions;
        this.skills = skills;
        this.hpBar = hpBar;
        this.mpBar = mpBar;
        this.mobHpBar = mobHpBar;
    }

    public void startBot() throws Exception {
        synchronized (lock) {
            if (!arduino.isPortOpen()) {
                log("Ошибка: порт Arduino не открыт");
                throw new Exception("Порт Arduino не открыт");
            }
        }

        running = true;
        new Thread(() -> {
            while (running) {
                try {
                    if (!isWindowActive(characterWindow)) {
                        log("Окно не активно, пропуск цикла");
                        Thread.sleep(2000);
                        continue;
                    }

                    // Выполняем поиск следующей цели
                    String targetKeys = getActionKeys("Поиск Моба");
                    if (targetKeys.isEmpty()) {
                        targetKeys = "F1"; // Дефолтная клавиша для поиска цели
                        log("⚠️ Используется дефолтная клавиша для поиска цели: F1");
                    }
                    synchronized (lock) {
                        for (String key : targetKeys.split(",")) {
                            arduino.sendCommand("PRESS_KEY:" + key.trim());
                            log("🔍 Выполняется поиск цели: " + key.trim());
                            Thread.sleep(300 + random.nextInt(100));
                        }
                    }

                    // Проверяем, найдена ли цель
                    double currentMobHP;
                    synchronized (lock) {
                        currentMobHP = screenReader.readBarLevel(mobHpBar[0], mobHpBar[1], mobHpBar[2], mobHpBar[3]);
                    }
                    if (currentMobHP <= 0.05) {
                        log("⚠️ Цель не найдена, повторный поиск...");
                        Thread.sleep(1000);
                        continue;
                    }
                    log("❤️ HP моба: " + String.format("%.1f%%", currentMobHP * 100));

                    // Выполняем атаку
                    String attackKeys = getActionKeys("Атака моба");
                    if (attackKeys.isEmpty()) {
                        attackKeys = "F2"; // Дефолтная клавиша для атаки
                        log("⚠️ Используется дефолтная клавиша для атаки: F2");
                    }
                    synchronized (lock) {
                        for (String key : attackKeys.split(",")) {
                            arduino.sendCommand("PRESS_KEY:" + key.trim());
                            log("⚔️ Атака начата: " + key.trim());
                            Thread.sleep(300 + random.nextInt(100));
                        }
                    }

                    // Основной цикл атаки
                    int attackAttempts = 0;
                    while (currentMobHP > 0.05 && attackAttempts < 8 && running) {
                        // Используем случайный скилл из списка
                        if (!skills.isEmpty()) {
                            Skill skill = skills.get(random.nextInt(skills.size()));
                            String skillKey = getSkillKey(skill.getName());
                            if (!skillKey.isEmpty()) {
                                synchronized (lock) {
                                    for (String key : skillKey.split(",")) {
                                        arduino.sendCommand("PRESS_KEY:" + key.trim());
                                        log("🪄 Использование скилла '" + skill.getName() + "': " + key.trim());
                                        Thread.sleep(300 + random.nextInt(100));
                                    }
                                }
                            }
                        }

                        // Повторяем атаку
                        synchronized (lock) {
                            for (String key : attackKeys.split(",")) {
                                arduino.sendCommand("PRESS_KEY:" + key.trim());
                                log("⚔️ Атака (" + (attackAttempts + 1) + "/8): " + key.trim());
                                Thread.sleep(300 + random.nextInt(100));
                            }
                        }

                        synchronized (lock) {
                            currentMobHP = screenReader.readBarLevel(mobHpBar[0], mobHpBar[1], mobHpBar[2], mobHpBar[3]);
                        }
                        log("❤️ HP моба после атаки: " + String.format("%.1f%%", currentMobHP * 100));

                        attackAttempts++;
                        Thread.sleep(500 + random.nextInt(200));
                    }

                    // Если моб мертв
                    if (currentMobHP <= 0.05) {
                        log("✅ Моб убит! Ждём 1 секунду...");
                        Thread.sleep(1000);

                        String deadKeys = getActionKeys("Моб убит");
                        if (!deadKeys.isEmpty()) {
                            synchronized (lock) {
                                for (String key : deadKeys.split(",")) {
                                    arduino.sendCommand("PRESS_KEY:" + key.trim());
                                    log("🔄 Действие после убийства: " + key.trim());
                                    Thread.sleep(300 + random.nextInt(100));
                                }
                            }
                        }
                    }

                    // Проверяем состояние персонажа
                    checkPlayerStatus();
                    Thread.sleep(1000 + random.nextInt(500));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log("❌ Ошибка в цикле бота: " + e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }).start();
    }

    private void checkPlayerStatus() {
        try {
            String mpKey = getActionKeys("Низкое MP");
            String hpKey = getActionKeys("Низкое HP");

            if (!mpKey.isEmpty()) {
                double playerMP;
                synchronized (lock) {
                    playerMP = screenReader.readBarLevel(mpBar[0], mpBar[1], mpBar[2], mpBar[3]);
                }
                if (playerMP < mpPercent) {
                    synchronized (lock) {
                        for (String key : mpKey.split(",")) {
                            arduino.sendCommand("PRESS_KEY:" + key.trim());
                            log("💧 Восстановление MP: " + key.trim());
                            Thread.sleep(300 + random.nextInt(100));
                        }
                    }
                }
            }

            if (!hpKey.isEmpty()) {
                double playerHP;
                synchronized (lock) {
                    playerHP = screenReader.readBarLevel(hpBar[0], hpBar[1], hpBar[2], hpBar[3]);
                }
                if (playerHP < hpPercent) {
                    synchronized (lock) {
                        for (String key : hpKey.split(",")) {
                            arduino.sendCommand("PRESS_KEY:" + key.trim());
                            log("❤️ Восстановление HP: " + key.trim());
                            Thread.sleep(300 + random.nextInt(100));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log("❌ Ошибка проверки HP/MP: " + e.getMessage());
        }
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

    private String getSkillKey(String skillName) {
        // Маппинг скиллов на клавиши (пример, можно настроить через UI или конфиг)
        // Для упрощения используем F3-F12 для скиллов
        int index = skills.indexOf(skills.stream().filter(s -> s.getName().equals(skillName)).findFirst().orElse(null));
        if (index >= 0 && index < 10) {
            return "F" + (3 + index);
        }
        return "";
    }

    private boolean isWindowActive(String windowTitle) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            WinDef.HWND hWnd = User32.INSTANCE.FindWindow(null, windowTitle);
            return hWnd != null && User32.INSTANCE.IsWindowVisible(hWnd);
        }
        try {
            return ProcessHandle.allProcesses()
                    .map(ProcessHandle::info)
                    .map(ProcessHandle.Info::command)
                    .filter(cmd -> cmd.isPresent() && cmd.get().contains(windowTitle))
                    .findAny()
                    .isPresent();
        } catch (Exception e) {
            log("Ошибка проверки активности окна: " + e.getMessage());
            return true;
        }
    }

    public void loadClassSkills(ClassId classId, ObservableList<Skill> skills) {
        skills.clear();
        List<Skill> classSkills = SkillList.getSkillsForClass(classId);
        if (classSkills.size() <= 4) {
            log("⚠️ Внимание: для класса " + classId.getDisplayName() + " загружены дефолтные скиллы. Проверьте SkillList.java.");
        } else {
            log("Загружено " + classSkills.size() + " скиллов для класса " + classId.getDisplayName() + ": " +
                    classSkills.stream().map(Skill::getName).collect(Collectors.joining(", ")));
        }
        skills.addAll(classSkills);
    }
}
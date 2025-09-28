package com.lineagebot;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class BotController {
    private final BotStats botStats = new BotStats();
    private long startTime;
    private final ScreenReader screenReader;
    private final ArduinoInterface arduino;
    private volatile boolean running = false;
    private final double hpPercent;
    private final double mpPercent;
    private final StringProperty log = new SimpleStringProperty("");
    private final String characterWindow;
    private final ObservableList<BotUIController.Action> actions;
    private final int[] hpBar;
    private final int[] mpBar;
    private final int[] mobHpBar;
    private final Random random = new Random();
    private final Map<BotUIController.Action, Long> lastActionTimes = new HashMap<>();
    private final Object arduinoLock = new Object();
    private final ReentrantReadWriteLock screenLock = new ReentrantReadWriteLock();

    // Групповое управление - новые поля
    private final ObservableList<SupportAction> supportActions = FXCollections.observableArrayList();
    private final Map<SupportAction, Long> lastSupportActionTimes = new ConcurrentHashMap<>();
    private String supportedCharacter; // Имя персонажа, которого поддерживаем
    private BotStats supportedStats; // Статистика поддерживаемого персонажа
    private Thread supportMonitorThread;

    public BotController(String arduinoPort, double hpPercent, double mpPercent, String characterWindow,
                         ObservableList<BotUIController.Action> actions, ObservableList<Skill> skills,
                         int[] hpBar, int[] mpBar, int[] mobHpBar) {
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

    public boolean isBotRunning() {
        return running;
    }

    public String getDebugInfo() {
        return String.format(
                "BotController: running=%b, botStats=%s, supportedCharacter=%s",
                running, botStats.toString(), supportedCharacter
        );
    }

    private double readHpLevel() throws ScreenReadException {
        screenLock.readLock().lock();
        try {
            double level = screenReader.readBarLevel(hpBar[0], hpBar[1], hpBar[2], hpBar[3]);
            System.out.println("HP level read: " + level + " from coordinates: " +
                    hpBar[0] + "," + hpBar[1] + "," + hpBar[2] + "," + hpBar[3]);
            return level;
        } catch (Exception e) {
            System.out.println("Error reading HP: " + e.getMessage());
            throw new ScreenReadException("HP read failed", e);
        }
    }

    private double readMpLevel() throws ScreenReadException {
        try {
            double level = screenReader.readBarLevel(mpBar[0], mpBar[1], mpBar[2], mpBar[3]);
            System.out.println("MP level read: " + level + " from coordinates: " +
                    mpBar[0] + "," + mpBar[1] + "," + mpBar[2] + "," + mpBar[3]);
            return level;
        } catch (Exception e) {
            System.out.println("Error reading MP: " + e.getMessage());
            throw new ScreenReadException("MP read failed", e);
        }
    }

    private double readMobHpLevel() throws ScreenReadException {
        screenLock.readLock().lock();
        try {
            return screenReader.readBarLevel(mobHpBar[0], mobHpBar[1], mobHpBar[2], mobHpBar[3]);
        } finally {
            screenLock.readLock().unlock();
        }
    }

    public void startBot() throws Exception {
        startTime = System.currentTimeMillis();
        botStats.setStatus("RUNNING");
        if (!arduino.isPortOpen()) {
            log("Ошибка: порт Arduino не открыт");
            throw new Exception("Порт Arduino не открыт");
        }

        running = true;

        // Запускаем мониторинг поддержки, если есть поддерживаемый персонаж
        if (supportedCharacter != null) {
            startSupportMonitoring();
        }

        new Thread(() -> {
            while (running) {
                try {
                    if (!isWindowActive(characterWindow)) {
                        log("Окно не активно, пропуск цикла");
                        Thread.sleep(2000);
                        continue;
                    }

                    double currentMobHP = readMobHpLevel();

                    List<BotUIController.Action> triggeredActions = checkPlayerStatus();
                    if (!triggeredActions.isEmpty()) {
                        for (BotUIController.Action action : triggeredActions) {
                            String keys = action.getKeys();
                            synchronized (arduinoLock) {
                                for (String key : keys.split(",")) {
                                    arduino.sendCommand("PRESS_KEY:" + key.trim());
                                    log("🪄 Приоритетный скилл '" + action.getActionType() + "' (" +
                                            ("Таймер n сек".equals(action.getCondition()) ? "Таймер " + action.getTimerSeconds() + " сек" : action.getCondition()) +
                                            "): " + key.trim());
                                    Thread.sleep(300 + random.nextInt(100));
                                }
                            }
                            if ("Таймер n сек".equals(action.getCondition())) {
                                lastActionTimes.put(action, System.currentTimeMillis());
                            }
                            Thread.sleep(500);
                        }
                        Thread.sleep(1000);
                        continue;
                    }

                    if (currentMobHP <= 0.05) {
                        String targetKey = getActionKeys("Next Target");
                        if (targetKey.isEmpty()) {
                            targetKey = "TAB";
                            log("⚠️ Используется дефолтная клавиша для поиска цели: TAB");
                        }
                        synchronized (arduinoLock) {
                            for (String key : targetKey.split(",")) {
                                arduino.sendCommand("PRESS_KEY:" + key.trim());
                                log("🔍 Поиск следующей цели: " + key.trim());
                                Thread.sleep(100 + random.nextInt(100));
                            }
                        }
                        Thread.sleep(100);
                        continue;
                    }

                    log("❤️ HP моба: " + String.format("%.1f%%", currentMobHP * 100));

                    int attackAttempts = 0;
                    while (currentMobHP > 0.05 && attackAttempts < 8 && running) {
                        String autoAttackKey = getActionKeys("Auto Attack");
                        if (!autoAttackKey.isEmpty()) {
                            synchronized (arduinoLock) {
                                for (String key : autoAttackKey.split(",")) {
                                    arduino.sendCommand("PRESS_KEY:" + key.trim());
                                    log("⚔️ Авто атака: " + key.trim());
                                    Thread.sleep(100 + random.nextInt(0));
                                }
                            }
                        } else {
                            log("⚠️ Auto Attack не назначен, пропуск атаки");
                        }

                        List<BotUIController.Action> availableSkills = actions.stream()
                                .filter(action -> !action.getActionType().equals("Auto Attack") &&
                                        !action.getActionType().equals("Next Target") &&
                                        !action.getActionType().equals("Low HP") &&
                                        !action.getActionType().equals("Low MP") &&
                                        action.getCondition().equals("Нет"))
                                .toList();
                        if (!availableSkills.isEmpty()) {
                            BotUIController.Action action = availableSkills.get(random.nextInt(availableSkills.size()));
                            String skillKey = action.getKeys();
                            synchronized (arduinoLock) {
                                for (String key : skillKey.split(",")) {
                                    arduino.sendCommand("PRESS_KEY:" + key.trim());
                                    log("🪄 Использование скилла '" + action.getActionType() + "': " + key.trim());
                                    Thread.sleep(100 + random.nextInt(100));
                                }
                            }
                        }

                        currentMobHP = readMobHpLevel();
                        log("❤️ HP моба после атаки: " + String.format("%.1f%%", currentMobHP * 100));

                        attackAttempts++;
                        Thread.sleep(100 + random.nextInt(100));
                    }

                    if (currentMobHP <= 0.05) {
                        botStats.incrementMobsKilled();
                        log("✅ Моб убит! Ждём 1 секунду...");
                        Thread.sleep(100);
                    }

                    Thread.sleep(200 + random.nextInt(100));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ScreenReadException e) {
                    log("❌ Критическая ошибка чтения с экрана: " + e.getMessage());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } catch (Exception e) {
                    log("❌ Общая ошибка в цикле бота: " + e.getMessage());
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

    private List<BotUIController.Action> checkPlayerStatus() {
        List<BotUIController.Action> triggeredActions = new ArrayList<>();
        try {
            double playerHP = readHpLevel();
            double playerMP = readMpLevel();

            String mpKey = getActionKeys("Low MP");
            String hpKey = getActionKeys("Low HP");

            if (!mpKey.isEmpty() && playerMP < mpPercent) {
                synchronized (arduinoLock) {
                    for (String key : mpKey.split(",")) {
                        arduino.sendCommand("PRESS_KEY:" + key.trim());
                        log("💧 Восстановление MP: " + key.trim());
                        Thread.sleep(300 + random.nextInt(100));
                    }
                }
            }

            if (!hpKey.isEmpty() && playerHP < hpPercent) {
                synchronized (arduinoLock) {
                    for (String key : hpKey.split(",")) {
                        arduino.sendCommand("PRESS_KEY:" + key.trim());
                        log("❤️ Восстановление HP: " + key.trim());
                        Thread.sleep(300 + random.nextInt(100));
                    }
                }
            }

            for (BotUIController.Action action : actions) {
                String condition = action.getCondition();
                if (condition.equals("HP < n%") && playerHP < hpPercent) {
                    triggeredActions.add(action);
                } else if (condition.equals("MP < n%") && playerMP < mpPercent) {
                    triggeredActions.add(action);
                } else if (condition.equals("Таймер n сек")) {
                    long currentTime = System.currentTimeMillis();
                    long lastTime = lastActionTimes.getOrDefault(action, 0L);
                    if (lastTime == 0 || currentTime - lastTime >= action.getTimerSeconds() * 1000L) {
                        triggeredActions.add(action);
                    }
                }
            }

        } catch (ScreenReadException e) {
            log("❌ Ошибка проверки HP/MP: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return triggeredActions;
    }

    public void stopBot() {
        botStats.setStatus("STOPPED");
        running = false;
        synchronized (arduinoLock) {
            arduino.close();
        }
        lastActionTimes.clear();
        clearSupportTimers();

        // Останавливаем мониторинг поддержки
        if (supportMonitorThread != null && supportMonitorThread.isAlive()) {
            supportMonitorThread.interrupt();
            try {
                supportMonitorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        supportedCharacter = null;
        supportedStats = null;
        log("Бот остановлен");
    }

    private void updateStats() {
        long currentUptime = System.currentTimeMillis() - startTime;
        botStats.setUptime(currentUptime);

        try {
            double hp = readHpLevel() * 100;
            double mp = readMpLevel() * 100;
            botStats.setCurrentHp(hp);
            botStats.setCurrentMp(mp);

            // Проверяем смерть персонажа
            boolean wasAlive = botStats.isAlive();
            boolean isNowAlive = hp > 5.0;
            botStats.setAlive(isNowAlive);

            if (wasAlive && !isNowAlive) {
                botStats.incrementDeaths();
                log("💀 Персонаж умер! Всего смертей: " + botStats.getDeaths());
            }
        } catch (ScreenReadException e) {
            log("❌ Ошибка обновления статистики: " + e.getMessage());
        }
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
            if (hWnd == null) return false;

            // Проверяем, является ли наше окно активным (foreground)
            WinDef.HWND foregroundHwnd = User32.INSTANCE.GetForegroundWindow();
            return hWnd.equals(foregroundHwnd) && User32.INSTANCE.IsWindowVisible(hWnd);
        }
        try {
            return ProcessHandle.allProcesses()
                    .map(ProcessHandle::info)
                    .map(ProcessHandle.Info::command)
                    .anyMatch(cmd -> cmd.isPresent() && cmd.get().contains(windowTitle));
        } catch (Exception e) {
            log("Ошибка проверки активности окна: " + e.getMessage());
            return true;
        }
    }

    public void loadClassSkills(ClassId classId, ObservableList<Skill> skills) {
        // Не используется
    }

    public BotStats getBotStats() {
        return botStats;
    }

    public void forceStatsUpdate() {
        if (!running) {
            // Если бот не запущен, устанавливаем значения по умолчанию
            botStats.setStatus("STOPPED");
            botStats.setCurrentHp(0);
            botStats.setCurrentMp(0);
            botStats.setAlive(false);
            botStats.setUptime(0);
            botStats.setDeaths(0);
            return;
        }

        try {
            // Убедимся, что статус RUNNING
            botStats.setStatus("RUNNING");

            // Читаем текущие уровни HP/MP с экрана
            double hpLevel = readHpLevel();
            double mpLevel = readMpLevel();

            // Обновляем статистику
            botStats.setCurrentHp(hpLevel * 100);
            botStats.setCurrentMp(mpLevel * 100);

            // Проверяем, жив ли персонаж (HP > 10%)
            boolean isAlive = hpLevel > 0.1;
            botStats.setAlive(isAlive);

            // Обновляем время работы
            if (startTime > 0) {
                botStats.setUptime(System.currentTimeMillis() - startTime);
            }

            System.out.println("Stats updated - HP: " + (hpLevel * 100) + "%, MP: " + (mpLevel * 100) + "%, Alive: " + isAlive);

        } catch (Exception e) {
            System.out.println("Error in forceStatsUpdate: " + e.getMessage());
            // Не устанавливаем статус ERROR, сохраняем предыдущие значения
        }
    }

    // Групповое управление - новые методы

    public void setSupportedCharacter(String characterName, BotStats stats) {
        this.supportedCharacter = characterName;
        this.supportedStats = stats;

        // Перезапускаем мониторинг поддержки при изменении поддерживаемого персонажа
        if (supportMonitorThread != null && supportMonitorThread.isAlive()) {
            supportMonitorThread.interrupt();
        }

        if (running && supportedCharacter != null) {
            startSupportMonitoring();
        }
    }

    private void startSupportMonitoring() {
        if (supportMonitorThread != null && supportMonitorThread.isAlive()) {
            supportMonitorThread.interrupt();
        }

        supportMonitorThread = new Thread(() -> {
            while (running && supportedCharacter != null && supportedStats != null) {
                try {
                    checkSupportActions();
                    Thread.sleep(1000); // Проверяем каждую секунду
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log("❌ Ошибка в мониторинге поддержки: " + e.getMessage());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        supportMonitorThread.setDaemon(true);
        supportMonitorThread.start();

        log("🎯 Мониторинг поддержки запущен для " + supportedCharacter);
    }

    public void checkSupportActions() {
        if (supportedStats == null || !running || supportedCharacter == null) return;

        try {
            for (SupportAction action : supportActions) {
                boolean shouldExecute = false;
                long currentTime = System.currentTimeMillis();
                long lastActionTime = getLastSupportActionTime(action);

                switch (action.getSupportType()) {
                    case HEAL_MAIN:
                        shouldExecute = supportedStats.getCurrentHp() < action.getTriggerValue() &&
                                (currentTime - lastActionTime) > 3000; // Не чаще чем раз в 3 секунды
                        break;
                    case MANA_TRANSFER:
                        shouldExecute = supportedStats.getCurrentMp() < action.getTriggerValue() &&
                                (currentTime - lastActionTime) > 5000; // Не чаще чем раз в 5 секунд
                        break;
                    case BUFF_MAIN:
                        // Баффы по таймеру (triggerValue в секундах)
                        long buffCooldown = (long) (action.getTriggerValue() * 1000);
                        shouldExecute = (currentTime - lastActionTime) > buffCooldown;
                        break;
                    case CURE_MAIN:
                        // Лечение дебаффов - проверяем по времени
                        shouldExecute = (currentTime - lastActionTime) > 8000; // Каждые 8 секунд
                        break;
                    case TARGET_ASSIST:
                        // Помощь в атаке - проверяем, что основной в бою
                        shouldExecute = supportedStats.getStatus().equals("RUNNING") &&
                                supportedStats.getCurrentHp() > 20.0 && // Только если у основного достаточно HP
                                (currentTime - lastActionTime) > 2000; // Каждые 2 секунды
                        break;
                    case EMERGENCY_RETREAT:
                        // Экстренное отступление при критическом HP основного
                        shouldExecute = supportedStats.getCurrentHp() < 15.0 &&
                                (currentTime - lastActionTime) > 10000; // Не чаще чем раз в 10 секунд
                        break;
                }

                if (shouldExecute) {
                    executeSupportAction(action);
                    updateLastSupportActionTime(action);

                    // Добавляем небольшую задержку между действиями поддержки
                    try {
                        Thread.sleep(200 + random.nextInt(200));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log("❌ Ошибка проверки действий поддержки: " + e.getMessage());
        }
    }

    private void executeSupportAction(SupportAction action) {
        synchronized (arduinoLock) {
            String[] keys = action.getActionKey().split(",");
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i].trim();
                arduino.sendCommand("PRESS_KEY:" + key);

                String actionType = action.getSupportType().getDisplayName();
                log("🎯 Поддержка " + supportedCharacter + ": " + actionType + " - " + key);

                // Добавляем задержку между последовательными нажатиями клавиш
                if (i < keys.length - 1) {
                    try {
                        Thread.sleep(150 + random.nextInt(100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private long getLastSupportActionTime(SupportAction action) {
        synchronized (lastSupportActionTimes) {
            return lastSupportActionTimes.getOrDefault(action, 0L);
        }
    }

    private void updateLastSupportActionTime(SupportAction action) {
        synchronized (lastSupportActionTimes) {
            lastSupportActionTimes.put(action, System.currentTimeMillis());
        }
    }

    private void clearSupportTimers() {
        synchronized (lastSupportActionTimes) {
            lastSupportActionTimes.clear();
        }
    }

    public void addSupportAction(SupportAction action) {
        supportActions.add(action);
        log("✅ Добавлено действие поддержки: " + action.getSupportType().getDisplayName());
    }

    public ObservableList<SupportAction> getSupportActions() {
        return supportActions;
    }

    public void clearSupportActions() {
        supportActions.clear();
        clearSupportTimers();
        log("🗑️ Все действия поддержки очищены");
    }

    public String getSupportedCharacter() {
        return supportedCharacter;
    }

    public boolean isSupporting() {
        return supportedCharacter != null && supportedStats != null;
    }

    public ObservableList<BotUIController.Action> getActions() {
        return actions;
    }
}




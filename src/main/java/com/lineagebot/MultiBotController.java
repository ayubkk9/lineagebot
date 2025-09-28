package com.lineagebot;

import javafx.collections.FXCollections;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MultiBotController {
    private final Map<String, BotController> controllers = new HashMap<>();
    private final Map<String, CharacterProfile> profiles = new HashMap<>();
    private String mainCharacter;
    private volatile boolean running = false;

    // Добавляем логгер для замены deprecated методов
    private void log(String message) {
        System.out.println("[MultiBot] " + message);
    }

    public void addCharacter(CharacterProfile profile) {
        profiles.put(profile.getCharacterName(), profile);

        BotController controller = new BotController(
                profile.getArduinoPort(),
                profile.getHpPercent(),
                profile.getMpPercent(),
                profile.getWindowTitle(),
                profile.getActions(),
                FXCollections.observableArrayList(),
                profile.getHpBar(),
                profile.getMpBar(),
                profile.getMobHpBar()
        );

        controllers.put(profile.getCharacterName(), controller);
        log("Добавлен персонаж: " + profile.getCharacterName());
    }

    public void removeCharacter(String characterName) {
        BotController controller = controllers.get(characterName);
        if (controller != null) {
            controller.stopBot();
            log("Остановлен и удален персонаж: " + characterName);
        }
        controllers.remove(characterName);
        profiles.remove(characterName);

        if (mainCharacter != null && mainCharacter.equals(characterName)) {
            mainCharacter = null;
            log("Удален основной персонаж: " + characterName);
        }
    }

    public void setMainCharacter(String characterName) {
        this.mainCharacter = characterName;

        // Настраиваем поддержку для всех персонажей
        BotStats mainStats = controllers.get(mainCharacter).getBotStats();

        for (String name : controllers.keySet()) {
            if (!name.equals(mainCharacter)) {
                controllers.get(name).setSupportedCharacter(mainCharacter, mainStats);
                profiles.get(name).setStatus("Поддержка: " + mainCharacter);
                log("Настроена поддержка для " + name + " -> " + mainCharacter);
            }
        }

        profiles.get(mainCharacter).setStatus("Основной");
        log("Установлен основной персонаж: " + mainCharacter);
    }

    public String getMainCharacter() {
        return mainCharacter;
    }

    public BotController getController(String characterName) {
        return controllers.get(characterName);
    }

    public void startGroupBot() throws Exception {
        if (running) {
            throw new Exception("Групповой бот уже запущен");
        }

        running = true;
        log("Запуск группового бота...");

        // Запускаем всех персонажей
        for (String name : controllers.keySet()) {
            BotController controller = controllers.get(name);
            if (controller != null) {
                try {
                    controller.startBot();
                    profiles.get(name).setStatus("Работает");
                    log("✅ Запущен персонаж: " + name);
                } catch (Exception e) {
                    log("❌ Ошибка запуска персонажа " + name + ": " + e.getMessage());
                    profiles.get(name).setStatus("Ошибка: " + e.getMessage());
                }
            }
        }

        log("Групповой бот успешно запущен");
    }

    public void stopGroupBot() {
        running = false;
        log("Остановка группового бота...");

        for (BotController controller : controllers.values()) {
            controller.stopBot();
        }

        for (CharacterProfile profile : profiles.values()) {
            profile.setStatus("Остановлен");
        }

        log("Групповой бот остановлен");
    }

    public boolean isRunning() {
        return running;
    }

    public List<CharacterProfile> getCharacterProfiles() {
        return new ArrayList<>(profiles.values());
    }

    public int getActiveCharactersCount() {
        return (int) profiles.values().stream()
                .filter(p -> "Работает".equals(p.getStatus()))
                .count();
    }
}
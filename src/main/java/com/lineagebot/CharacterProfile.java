package com.lineagebot;

import javafx.beans.property.*;
import javafx.collections.ObservableList;

public class CharacterProfile {
    private String characterName;
    private String windowTitle;
    private String arduinoPort;
    private double hpPercent;
    private double mpPercent;
    private int[] hpBar;
    private int[] mpBar;
    private int[] mobHpBar;
    private ObservableList<BotUIController.Action> actions;
    private ClassId classId;
    private boolean isMain;
    private String status;

    // JavaFX properties - ВСЕ ИНИЦИАЛИЗИРУЕМ!
    private final StringProperty characterNameProperty = new SimpleStringProperty("");
    private final StringProperty windowTitleProperty = new SimpleStringProperty("");
    private final StringProperty statusProperty = new SimpleStringProperty("");
    private final StringProperty classNameProperty = new SimpleStringProperty("");
    private final BooleanProperty mainProperty = new SimpleBooleanProperty(false);

    public CharacterProfile(String characterName, String windowTitle, String arduinoPort,
                            double hpPercent, double mpPercent, int[] hpBar, int[] mpBar,
                            int[] mobHpBar, ObservableList<BotUIController.Action> actions) {
        this.characterName = characterName;
        this.windowTitle = windowTitle;
        this.arduinoPort = arduinoPort;
        this.hpPercent = hpPercent;
        this.mpPercent = mpPercent;
        this.hpBar = hpBar;
        this.mpBar = mpBar;
        this.mobHpBar = mobHpBar;
        this.actions = actions;
        this.isMain = false;
        this.status = "Остановлен";
        this.classId = null;

        // Инициализация свойств значениями
        this.characterNameProperty.set(characterName);
        this.windowTitleProperty.set(windowTitle);
        this.statusProperty.set("Остановлен");
        this.mainProperty.set(false);
        this.classNameProperty.set("");
    }

    // Геттеры для JavaFX properties
    public StringProperty characterNameProperty() { return characterNameProperty; }
    public StringProperty windowTitleProperty() { return windowTitleProperty; }
    public StringProperty statusProperty() { return statusProperty; }
    public BooleanProperty mainProperty() { return mainProperty; }
    public StringProperty classNameProperty() { return classNameProperty; }

    // Обновленные сеттеры
    public void setMain(boolean main) {
        this.isMain = main;
        mainProperty.set(main);
    }

    public void setStatus(String status) {
        this.status = status;
        statusProperty.set(status);
    }

    public void setClassId(ClassId classId) {
        this.classId = classId;
        this.classNameProperty.set(classId != null ? classId.getDisplayName() : "");
    }

    // Обычные геттеры
    public String getCharacterName() { return characterName; }
    public String getWindowTitle() { return windowTitle; }
    public String getArduinoPort() { return arduinoPort; }
    public double getHpPercent() { return hpPercent; }
    public double getMpPercent() { return mpPercent; }
    public int[] getHpBar() { return hpBar; }
    public int[] getMpBar() { return mpBar; }
    public int[] getMobHpBar() { return mobHpBar; }
    public ObservableList<BotUIController.Action> getActions() { return actions; }
    public ClassId getClassId() { return classId; }
    public boolean isMain() { return isMain; }
    public String getStatus() { return status; }
    public String getClassName() { return classNameProperty.get(); }

    @Override
    public String toString() {
        return characterName + (isMain ? " (Основной)" : " (Вспомогательный)");
    }
}
package com.lineagebot;

import javafx.beans.property.*;

public class SupportAction {
    public enum SupportType {
        HEAL_MAIN("Лечение основного"),
        BUFF_MAIN("Бафф основного"),
        CURE_MAIN("Снятие эффектов"),
        MANA_TRANSFER("Передача маны"),
        TARGET_ASSIST("Помощь в атаке"),
        EMERGENCY_RETREAT("Экстренное отступление");

        private final String displayName;

        SupportType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final SupportType supportType;
    private final String actionKey;
    private final double triggerValue;
    private final int priority;
    private final ObjectProperty<SupportType> supportTypeProperty = new SimpleObjectProperty<>();
    private final StringProperty actionKeyProperty = new SimpleStringProperty();
    private final DoubleProperty triggerValueProperty = new SimpleDoubleProperty();
    private final IntegerProperty priorityProperty = new SimpleIntegerProperty();

    public SupportAction(SupportType supportType, String actionKey, double triggerValue, int priority) {
        this.supportType = supportType;
        this.actionKey = actionKey;
        this.triggerValue = triggerValue;
        this.priority = priority;
    }

    // Геттеры
    public SupportType getSupportType() { return supportType; }
    public String getActionKey() { return actionKey; }
    public double getTriggerValue() { return triggerValue; }
    public int getPriority() { return priority; }

    @Override
    public String toString() {
        return supportType.getDisplayName() + " (" + actionKey + ") при " + triggerValue + "%";
    }
}
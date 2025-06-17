package com.lineagebot;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Skill {
    private final SimpleStringProperty name;
    private final SimpleStringProperty type;
    private final SimpleIntegerProperty mpCost;
    private final SimpleIntegerProperty cooldown;
    private final String keys;

    public Skill(String name, String type, int mpCost, int cooldown, String keys) {
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        this.mpCost = new SimpleIntegerProperty(mpCost);
        this.cooldown = new SimpleIntegerProperty(cooldown);
        this.keys = keys;
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public String getType() {
        return type.get();
    }

    public SimpleStringProperty typeProperty() {
        return type;
    }

    public int getMpCost() {
        return mpCost.get();
    }

    public SimpleIntegerProperty mpCostProperty() {
        return mpCost;
    }

    public int getCooldown() {
        return cooldown.get();
    }

    public SimpleIntegerProperty cooldownProperty() {
        return cooldown;
    }

    public String getKeys() {
        return keys;
    }
}
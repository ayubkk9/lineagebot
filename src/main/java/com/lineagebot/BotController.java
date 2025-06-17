package com.lineagebot;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BotController {
    public enum SkillType {
        ATTACK("Атака", "#FF6B6B"),
        HEAL("Лечение", "#4CAF50"),
        BUFF("Бафф", "#FFC107"),
        DEBUFF("Дебафф", "#9C27B0"),
        SUMMON("Призыв", "#00BCD4"),
        OTHER("Другое", "#9E9E9E");

        private final String name;
        private final String color;

        SkillType(String name, String color) {
            this.name = name;
            this.color = color;
        }

        public String getName() { return name; }
        public String getColor() { return color; }
        public Color getColorObj() { return Color.web(color); }
    }

    public static class SkillDisplay {
        private final String name;
        private final int level;
        private final SkillType type;
        private final int mpCost;
        private final boolean isActive;

        public SkillDisplay(String name, int level, SkillType type, int mpCost, boolean isActive) {
            this.name = name;
            this.level = level;
            this.type = type;
            this.mpCost = mpCost;
            this.isActive = isActive;
        }

        public String getName() { return name; }
        public int getLevel() { return level; }
        public SkillType getType() { return type; }
        public String getTypeName() { return type.getName(); }
        public String getTypeColor() { return type.getColor(); }
        public int getMpCost() { return mpCost; }
        public boolean isActive() { return isActive; }
    }

    private final Map<Integer, Skill> availableSkills = new HashMap<>();
    private final ObservableList<SkillDisplay> observableSkills = FXCollections.observableArrayList();
    private final ObservableList<String> currentStrategy = FXCollections.observableArrayList();
    private ClassId currentClass;
    private List<SkillType> combatPriority = new ArrayList<>();
    private final StringProperty log = new SimpleStringProperty();
    private volatile boolean running = false;
    private final Object lock = new Object();
    private final ArduinoInterface arduino;
    private final ScreenReader screenReader;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private double currentHpPercent = 100;
    private double currentMpPercent = 100;
    private double hpThreshold = 30;
    private double mpThreshold = 30;
    private String activeWindowTitle;
    private boolean needsHealing = false;
    private boolean needsMana = false;
    private long lastSkillUsedTime = 0;
    private final Map<Integer, Long> skillCooldowns = new HashMap<>();

    public BotController(String arduinoPort, Consumer<String> logCallback) {
        this.arduino = new ArduinoInterface(arduinoPort, logCallback);
        this.screenReader = new ScreenReader();
        startStatusMonitoring();
    }

    private void startStatusMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            if (running) {
                checkCharacterStatus();
                performCombatRotation();
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void checkCharacterStatus() {
        needsHealing = currentHpPercent < hpThreshold;
        needsMana = currentMpPercent < mpThreshold;

        if (needsHealing) {
            log("Требуется лечение! HP: " + currentHpPercent + "%");
        }
    }

    public void setHpThreshold(double threshold) {
        this.hpThreshold = threshold;
    }

    public void setMpThreshold(double threshold) {
        this.mpThreshold = threshold;
    }

    public void setActiveWindow(String windowTitle) {
        this.activeWindowTitle = windowTitle;
    }

    public void updateHpMpValues(double hpPercent, double mpPercent) {
        this.currentHpPercent = hpPercent;
        this.currentMpPercent = mpPercent;
    }

    public void performCombatRotation() {
        if (!running) return;

        if (needsHealing) {
            findAndUseSkill(SkillType.HEAL);
            return;
        }

        if (needsMana) {
            findAndUseSkill(SkillType.BUFF);
            return;
        }

        getSkillsByPriority().stream()
                .filter(this::canUseSkill)
                .findFirst()
                .ifPresent(this::useSkill);
    }

    private boolean canUseSkill(Skill skill) {
        if (skillCooldowns.containsKey(skill.getId())) {
            long cooldownEnd = skillCooldowns.get(skill.getId());
            if (System.currentTimeMillis() < cooldownEnd) {
                return false;
            }
        }

        if (skill.getMpConsume() > currentMpPercent) {
            return false;
        }

        return true;
    }

    private void useSkill(Skill skill) {
        String keys = mapSkillToKeys(skill);
        if (keys != null && !keys.isEmpty()) {
            if (arduino.sendCommand("PRESS_KEY:" + keys)) {
                log("Использован скилл: " + skill.getName());
                lastSkillUsedTime = System.currentTimeMillis();
                skillCooldowns.put(skill.getId(), lastSkillUsedTime + skill.getReuseDelay());
            }
        }
    }

    private String mapSkillToKeys(Skill skill) {
        return "F" + (skill.getId() % 12 + 1);
    }

    private void findAndUseSkill(SkillType type) {
        observableSkills.stream()
                .filter(skill -> skill.getType() == type && skill.isActive())
                .findFirst()
                .ifPresent(skill -> useSkill(availableSkills.get(skill.getName())));
    }

    public void addSkill(Skill skill) {
        availableSkills.put(skill.getId(), skill);
        updateSkillDisplay();
    }

    public void loadClassSkills(ClassId classId) {
        currentClass = classId;
        availableSkills.clear();

        // Реальные скиллы для классов
        switch (classId) {
            case DARK_AVENGER:
                availableSkills.put(1, new Skill(1, "Power Strike", 1, 20, 2000));
                availableSkills.put(2, new Skill(2, "Drain Health", 1, 30, 5000));
                availableSkills.put(3, new Skill(3, "Shield Stun", 1, 25, 8000));
                break;
            case PALADIN:
                availableSkills.put(1, new Skill(1, "Holy Strike", 1, 25, 3000));
                availableSkills.put(2, new Skill(2, "Heal", 1, 40, 6000));
                availableSkills.put(3, new Skill(3, "Might", 1, 30, 10000));
                break;
            case BISHOP:
                availableSkills.put(1, new Skill(1, "Heal", 1, 30, 5000));
                availableSkills.put(2, new Skill(2, "Resurrection", 1, 50, 30000));
                availableSkills.put(3, new Skill(3, "Blessing", 1, 40, 15000));
                break;
            case SPELLSINGER:
                availableSkills.put(1, new Skill(1, "Ice Bolt", 1, 20, 2000));
                availableSkills.put(2, new Skill(2, "Blizzard", 1, 35, 7000));
                availableSkills.put(3, new Skill(3, "Mana Regeneration", 1, 30, 12000));
                break;
            // Добавьте другие классы по аналогии
            default:
                availableSkills.put(1, new Skill(1, "Атака", 1, 20, 2000));
                availableSkills.put(2, new Skill(2, "Лечение", 1, 30, 5000));
                availableSkills.put(3, new Skill(3, "Бафф", 1, 40, 10000));
        }

        configureClassStrategy(classId);
        updateSkillDisplay();
    }

    private void updateSkillDisplay() {
        observableSkills.setAll(
                availableSkills.values().stream()
                        .map(skill -> new SkillDisplay(
                                skill.getName(),
                                skill.getLevel(),
                                determineSkillType(skill),
                                skill.getMpConsume(),
                                isSkillActive(skill)
                        ))
                        .sorted(Comparator.comparing(s -> combatPriority.indexOf(s.getType())))
                        .collect(Collectors.toList())
        );

        updateStrategyDisplay();
    }

    private void updateStrategyDisplay() {
        currentStrategy.setAll(
                combatPriority.stream()
                        .map(SkillType::getName)
                        .collect(Collectors.toList())
        );
    }

    private SkillType determineSkillType(Skill skill) {
        String name = skill.getName().toLowerCase();
        if (name.contains("strike") || name.contains("bolt") || name.contains("attack")) return SkillType.ATTACK;
        if (name.contains("heal") || name.contains("resurrection")) return SkillType.HEAL;
        if (name.contains("blessing") || name.contains("might") || name.contains("regeneration")) return SkillType.BUFF;
        if (name.contains("stun") || name.contains("blizzard")) return SkillType.DEBUFF;
        return SkillType.OTHER;
    }

    private boolean isSkillActive(Skill skill) {
        if (skillCooldowns.containsKey(skill.getId())) {
            return System.currentTimeMillis() > skillCooldowns.get(skill.getId());
        }
        return true;
    }

    private void configureClassStrategy(ClassId classId) {
        switch (classId) {
            case DARK_AVENGER:
            case PALADIN:
                combatPriority = Arrays.asList(SkillType.BUFF, SkillType.ATTACK, SkillType.HEAL);
                break;
            case SPELLSINGER:
            case SPELLHOWLER:
                combatPriority = Arrays.asList(SkillType.BUFF, SkillType.DEBUFF, SkillType.ATTACK);
                break;
            case BISHOP:
                combatPriority = Arrays.asList(SkillType.BUFF, SkillType.HEAL, SkillType.ATTACK);
                break;
            default:
                combatPriority = Arrays.asList(SkillType.ATTACK, SkillType.BUFF, SkillType.HEAL);
        }
    }

    public void setCombatPriority(List<SkillType> priority) {
        this.combatPriority = new ArrayList<>(priority);
        updateSkillDisplay();
    }

    public List<Skill> getSkillsByPriority() {
        return availableSkills.values().stream()
                .sorted(Comparator.comparingInt(skill ->
                        combatPriority.indexOf(determineSkillType(skill))))
                .collect(Collectors.toList());
    }

    public ObservableList<SkillDisplay> getObservableSkills() {
        return observableSkills;
    }

    public ObservableList<String> getCurrentStrategy() {
        return currentStrategy;
    }

    public List<SkillType> getAvailableSkillTypes() {
        return Arrays.asList(SkillType.values());
    }

    public void log(String message) {
        Platform.runLater(() -> log.set(log.get() + message + "\n"));
    }

    public StringProperty logProperty() {
        return log;
    }

    public void startBot() {
        if (!running) {
            running = true;
            log("Бот запущен для окна: " + activeWindowTitle);
        }
    }

    public void stopBot() {
        if (running) {
            running = false;
            log("Бот остановлен");
        }
    }

    public void shutdown() {
        stopBot();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        arduino.close();
    }

    public static class Skill {
        private final int id;
        private final String name;
        private final int level;
        private final int mpConsume;
        private final long reuseDelay;

        public Skill(int id, String name, int level, int mpConsume, long reuseDelay) {
            this.id = id;
            this.name = name;
            this.level = level;
            this.mpConsume = mpConsume;
            this.reuseDelay = reuseDelay;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public int getLevel() { return level; }
        public int getMpConsume() { return mpConsume; }
        public long getReuseDelay() { return reuseDelay; }
    }
}
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
    private final ObservableList<Skill> selectedSkills;
    private final int[] hpBar;
    private final int[] mpBar;
    private final int[] mobHpBar;
    private final Object lock = new Object();

    public BotController(String arduinoPort, double hpPercent, double mpPercent, String characterWindow,
                         ObservableList<BotUIController.Action> actions, ObservableList<Skill> selectedSkills,
                         int[] hpBar, int[] mpBar, int[] mobHpBar) {
        this.screenReader = new ScreenReader();
        this.arduino = new ArduinoInterface(arduinoPort, this::log);
        this.hpPercent = hpPercent / 100.0;
        this.mpPercent = mpPercent / 100.0;
        this.characterWindow = characterWindow;
        this.actions = actions;
        this.selectedSkills = selectedSkills;
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
                        log("Окно не активно, пропуск цикла");
                        Thread.sleep(2000);
                        continue;
                    }

                    // Поиск нового моба
                    String searchKeys = getActionKeys("Поиск Моба");
                    if (!searchKeys.isEmpty()) {
                        synchronized (lock) {
                            for (String key : searchKeys.split(",")) {
                                arduino.sendCommand("PRESS_KEY:" + key.trim());
                                log("🔍 Поиск цели: " + key.trim());
                                Thread.sleep(200);
                            }
                        }
                    }

                    // Основной цикл атаки
                    int attackAttempts = 0;
                    double currentMobHP;
                    synchronized (lock) {
                        currentMobHP = screenReader.readBarLevel(mobHpBar[0], mobHpBar[1], mobHpBar[2], mobHpBar[3]);
                    }
                    log("❤️ HP моба: " + String.format("%.1f%%", currentMobHP * 100));

                    // Атакуем, используя скиллы
                    int skillIndex = 0;
                    while (currentMobHP > 0.05 && attackAttempts < 8 && running) {
                        if (!selectedSkills.isEmpty()) {
                            Skill skill = selectedSkills.get(skillIndex % selectedSkills.size());
                            synchronized (lock) {
                                for (String key : skill.getKeys().split(",")) {
                                    arduino.sendCommand("PRESS_KEY:" + key.trim());
                                    log("⚔️ Использование скилла (" + (attackAttempts + 1) + "/8): " + skill.getName() + " (" + key.trim() + ")");
                                    Thread.sleep(300);
                                }
                            }
                            Thread.sleep(skill.getCooldown());
                            skillIndex++;
                        } else {
                            String attackKeys = getActionKeys("Атака моба");
                            if (!attackKeys.isEmpty()) {
                                synchronized (lock) {
                                    for (String key : attackKeys.split(",")) {
                                        arduino.sendCommand("PRESS_KEY:" + key.trim());
                                        log("⚔️ Атака (" + (attackAttempts + 1) + "/8): " + key.trim());
                                        Thread.sleep(300);
                                    }
                                }
                            }
                        }

                        synchronized (lock) {
                            currentMobHP = screenReader.readBarLevel(mobHpBar[0], mobHpBar[1], mobHpBar[2], mobHpBar[3]);
                        }
                        log("❤️ HP моба после атаки: " + String.format("%.1f%%", currentMobHP * 100));

                        attackAttempts++;
                        Thread.sleep(500);
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
                                    Thread.sleep(300);
                                }
                            }
                        }
                    }

                    // Проверяем состояние персонажа
                    checkPlayerStatus();
                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log("❌ Ошибка в цикле бота: " + e.getMessage());
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
                        arduino.sendCommand("PRESS_KEY:" + mpKey);
                    }
                    log("💧 Восстановление MP: " + mpKey);
                }
            }

            if (!hpKey.isEmpty()) {
                double playerHP;
                synchronized (lock) {
                    playerHP = screenReader.readBarLevel(hpBar[0], hpBar[1], hpBar[2], hpBar[3]);
                }
                if (playerHP < hpPercent) {
                    synchronized (lock) {
                        arduino.sendCommand("PRESS_KEY:" + hpKey);
                    }
                    log("❤️ Восстановление HP: " + hpKey);
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
        switch (classId) {
            case DARK_AVENGER:
                skills.add(new Skill("Power Strike", "Атака", 20, 2000, "F1"));
                skills.add(new Skill("Drain Health", "Атака", 30, 5000, "F2"));
                skills.add(new Skill("Shield Stun", "Дебафф", 25, 8000, "F3"));
                skills.add(new Skill("Deflect Arrow", "Бафф", 15, 10000, "F4"));
                break;
            case PALADIN:
                skills.add(new Skill("Holy Strike", "Атака", 25, 3000, "F1"));
                skills.add(new Skill("Heal", "Лечение", 40, 6000, "F2"));
                skills.add(new Skill("Might", "Бафф", 30, 10000, "F3"));
                skills.add(new Skill("Holy Armor", "Бафф", 20, 12000, "F4"));
                break;
            case GLADIATOR:
                skills.add(new Skill("Triple Slash", "Атака", 25, 3000, "F1"));
                skills.add(new Skill("Sonic Focus", "Бафф", 10, 5000, "F2"));
                skills.add(new Skill("War Cry", "Бафф", 30, 15000, "F3"));
                skills.add(new Skill("Double Sonic Slash", "Атака", 35, 7000, "F4"));
                break;
            case BISHOP:
                skills.add(new Skill("Heal", "Лечение", 30, 5000, "F1"));
                skills.add(new Skill("Resurrection", "Лечение", 50, 30000, "F2"));
                skills.add(new Skill("Blessing of Queen", "Бафф", 40, 15000, "F3"));
                skills.add(new Skill("Greater Heal", "Лечение", 45, 8000, "F4"));
                break;
            case SPELLSINGER:
                skills.add(new Skill("Ice Bolt", "Атака", 20, 2000, "F1"));
                skills.add(new Skill("Blizzard", "Атака", 35, 7000, "F2"));
                skills.add(new Skill("Mana Regeneration", "Бафф", 30, 12000, "F3"));
                skills.add(new Skill("Frost Wall", "Дебафф", 40, 9000, "F4"));
                break;
            case SPELLHOWLER:
                skills.add(new Skill("Wind Strike", "Атака", 20, 2000, "F1"));
                skills.add(new Skill("Hurricane", "Атака", 35, 7000, "F2"));
                skills.add(new Skill("Curse Fear", "Дебафф", 30, 10000, "F3"));
                skills.add(new Skill("Vampiric Claw", "Атака", 40, 8000, "F4"));
                break;
            case NECROMANCER:
                skills.add(new Skill("Corpse Life Drain", "Атака", 25, 4000, "F1"));
                skills.add(new Skill("Summon Zombie", "Бафф", 50, 20000, "F2"));
                skills.add(new Skill("Curse Poison", "Дебафф", 30, 10000, "F3"));
                skills.add(new Skill("Death Spike", "Атака", 35, 6000, "F4"));
                break;
            case PROPHET:
                skills.add(new Skill("Blessing of Might", "Бафф", 30, 15000, "F1"));
                skills.add(new Skill("Heal", "Лечение", 40, 6000, "F2"));
                skills.add(new Skill("Wind Walk", "Бафф", 25, 12000, "F3"));
                skills.add(new Skill("Resist Shock", "Бафф", 20, 10000, "F4"));
                break;
            case SWORD_SINGER:
                skills.add(new Skill("Song of Hunter", "Бафф", 30, 15000, "F1"));
                skills.add(new Skill("Sonic Blaster", "Атака", 25, 4000, "F2"));
                skills.add(new Skill("Song of Wind", "Бафф", 30, 15000, "F3"));
                skills.add(new Skill("Song of Vitality", "Бафф", 35, 12000, "F4"));
                break;
            case TEMPLE_KNIGHT:
                skills.add(new Skill("Shield Bash", "Атака", 20, 5000, "F1"));
                skills.add(new Skill("Summon Storm Cubic", "Бафф", 40, 20000, "F2"));
                skills.add(new Skill("Deflect Arrow", "Бафф", 15, 10000, "F3"));
                skills.add(new Skill("Holy Armor", "Бафф", 25, 12000, "F4"));
                break;
            case SHILLIEN_KNIGHT:
                skills.add(new Skill("Drain Health", "Атака", 30, 5000, "F1"));
                skills.add(new Skill("Lightning Strike", "Атака", 35, 7000, "F2"));
                skills.add(new Skill("Shield Stun", "Дебафф", 25, 8000, "F3"));
                skills.add(new Skill("Hex", "Дебафф", 20, 10000, "F4"));
                break;
            case BLADEDANCER:
                skills.add(new Skill("Dance of Fire", "Бафф", 30, 15000, "F1"));
                skills.add(new Skill("Sting", "Атака", 25, 3000, "F2"));
                skills.add(new Skill("Dance of Fury", "Бафф", 30, 15000, "F3"));
                skills.add(new Skill("Dance of Light", "Бафф", 35, 12000, "F4"));
                break;
            case HAWKEYE:
                skills.add(new Skill("Power Shot", "Атака", 25, 3000, "F1"));
                skills.add(new Skill("Burst Shot", "Атака", 35, 6000, "F2"));
                skills.add(new Skill("Hawk Eye", "Бафф", 20, 10000, "F3"));
                skills.add(new Skill("Stunning Shot", "Дебафф", 30, 8000, "F4"));
                break;
            case SILVER_RANGER:
                skills.add(new Skill("Double Shot", "Атака", 25, 3000, "F1"));
                skills.add(new Skill("Elemental Shot", "Атака", 35, 6000, "F2"));
                skills.add(new Skill("Rapid Shot", "Бафф", 20, 10000, "F3"));
                skills.add(new Skill("Stunning Shot", "Дебафф", 30, 8000, "F4"));
                break;
            case PHANTOM_RANGER:
                skills.add(new Skill("Double Shot", "Атака", 25, 3000, "F1"));
                skills.add(new Skill("Lethal Shot", "Атака", 40, 7000, "F2"));
                skills.add(new Skill("Dead Eye", "Бафф", 20, 10000, "F3"));
                skills.add(new Skill("Stunning Shot", "Дебафф", 30, 8000, "F4"));
                break;
            case ABYSS_WALKER:
                skills.add(new Skill("Backstab", "Атака", 30, 4000, "F1"));
                skills.add(new Skill("Deadly Blow", "Атака", 25, 3000, "F2"));
                skills.add(new Skill("Shadow Step", "Бафф", 20, 10000, "F3"));
                skills.add(new Skill("Blinding Blow", "Атака", 35, 6000, "F4"));
                break;
            case PLAINS_WALKER:
                skills.add(new Skill("Backstab", "Атака", 30, 4000, "F1"));
                skills.add(new Skill("Deadly Blow", "Атака", 25, 3000, "F2"));
                skills.add(new Skill("Dash", "Бафф", 20, 10000, "F3"));
                skills.add(new Skill("Critical Blow", "Атака", 35, 6000, "F4"));
                break;
            case TREASURE_HUNTER:
                skills.add(new Skill("Backstab", "Атака", 30, 4000, "F1"));
                skills.add(new Skill("Deadly Blow", "Атака", 25, 3000, "F2"));
                skills.add(new Skill("Unlock", "Бафф", 15, 8000, "F3"));
                skills.add(new Skill("Fake Death", "Бафф", 20, 12000, "F4"));
                break;
            case WARLOCK:
                skills.add(new Skill("Summon Kat the Cat", "Бафф", 50, 20000, "F1"));
                skills.add(new Skill("Transfer Pain", "Бафф", 20, 10000, "F2"));
                skills.add(new Skill("Body to Mind", "Лечение", 0, 15000, "F3"));
                skills.add(new Skill("Servitor Heal", "Лечение", 30, 6000, "F4"));
                break;
            case SORCERER:
                skills.add(new Skill("Flame Strike", "Атака", 25, 3000, "F1"));
                skills.add(new Skill("Blazing Circle", "Атака", 35, 7000, "F2"));
                skills.add(new Skill("Prominence", "Атака", 40, 8000, "F3"));
                skills.add(new Skill("Mana Regeneration", "Бафф", 30, 12000, "F4"));
                break;
            case WARCRYER:
                skills.add(new Skill("Chant of Fire", "Бафф", 30, 15000, "F1"));
                skills.add(new Skill("Chant of Battle", "Бафф", 30, 15000, "F2"));
                skills.add(new Skill("Chant of Shielding", "Бафф", 30, 15000, "F3"));
                skills.add(new Skill("Soul Cry", "Бафф", 20, 10000, "F4"));
                break;
            case OVERLORD:
                skills.add(new Skill("Seal of Poison", "Дебафф", 30, 10000, "F1"));
                skills.add(new Skill("Seal of Flame", "Дебафф", 35, 12000, "F2"));
                skills.add(new Skill("Chant of Victory", "Бафф", 40, 15000, "F3"));
                skills.add(new Skill("Seal of Silence", "Дебафф", 30, 10000, "F4"));
                break;
            case DESTROYER:
                skills.add(new Skill("Power Smash", "Атака", 25, 3000, "F1"));
                skills.add(new Skill("Frenzy", "Бафф", 20, 15000, "F2"));
                skills.add(new Skill("Guts", "Бафф", 20, 15000, "F3"));
                skills.add(new Skill("Fatal Strike", "Атака", 30, 5000, "F4"));
                break;
            case TYRANT:
                skills.add(new Skill("Force Blaster", "Атака", 25, 3000, "F1"));
                skills.add(new Skill("Burning Fist", "Атака", 30, 5000, "F2"));
                skills.add(new Skill("Force Storm", "Атака", 35, 7000, "F3"));
                skills.add(new Skill("Hawk Fist", "Атака", 20, 10000, "F4"));
                break;
            case BOUNTY_HUNTER:
                skills.add(new Skill("Spoil", "Дебафф", 20, 8000, "F1"));
                skills.add(new Skill("Crushing Strike", "Атака", 25, 3000, "F2"));
                skills.add(new Skill("Sweeper", "Бафф", 15, 5000, "F3"));
                skills.add(new Skill("Fake Death", "Бафф", 20, 12000, "F4"));
                break;
            case WARSMITH:
                skills.add(new Skill("Whirlwind", "Атака", 25, 3000, "F1"));
                skills.add(new Skill("Summon Mechanic Golem", "Бафф", 50, 20000, "F2"));
                skills.add(new Skill("Repair", "Лечение", 20, 10000, "F3"));
                skills.add(new Skill("Hammer Crush", "Атака", 30, 5000, "F4"));
                break;
            default:
                skills.add(new Skill("Default Attack", "Атака", 20, 2000, "F1"));
                skills.add(new Skill("Default Heal", "Лечение", 30, 5000, "F2"));
                skills.add(new Skill("Default Buff", "Бафф", 40, 10000, "F3"));
                skills.add(new Skill("Default Debuff", "Дебафф", 25, 8000, "F4"));
        }
        log("Загружено " + skills.size() + " скиллов для класса: " + classId.getDisplayName());
    }
}
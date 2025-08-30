package com.lineagebot;

public class BotStats {
    private int mobsKilled;
    private boolean isAlive;
    private double currentHp;
    private double currentMp;
    private String status;
    private long uptime;
    private int deaths; // Добавляем поле deaths

    public BotStats() {
        this.mobsKilled = 0;
        this.isAlive = true;
        this.currentHp = 100.0;
        this.currentMp = 100.0;
        this.status = "STOPPED";
        this.uptime = 0;
        this.deaths = 0; // Инициализируем deaths
    }

    // Геттеры и сеттеры
    public synchronized int getMobsKilled() { return mobsKilled; }
    public synchronized void setMobsKilled(int mobsKilled) { this.mobsKilled = mobsKilled; }
    public synchronized void incrementMobsKilled() { mobsKilled++; }

    public synchronized boolean isAlive() { return isAlive; }
    public synchronized void setAlive(boolean alive) { isAlive = alive; }

    public synchronized double getCurrentHp() { return currentHp; }
    public synchronized void setCurrentHp(double hp) { currentHp = hp; }

    public synchronized double getCurrentMp() { return currentMp; }
    public synchronized void setCurrentMp(double mp) { currentMp = mp; }

    public synchronized String getStatus() { return status; }
    public synchronized void setStatus(String status) { this.status = status; }

    public synchronized long getUptime() { return uptime; }
    public synchronized void setUptime(long uptime) { this.uptime = uptime; }

    // Методы для deaths
    public synchronized int getDeaths() { return deaths; }
    public synchronized void setDeaths(int deaths) { this.deaths = deaths; }
    public synchronized void incrementDeaths() { deaths++; }

    // Метод для сброса статистики
    public synchronized void reset() {
        mobsKilled = 0;
        isAlive = true;
        currentHp = 100.0;
        currentMp = 100.0;
        status = "STOPPED";
        uptime = 0;
        deaths = 0;
    }

    @Override
    public synchronized String toString() {
        return String.format(
                "BotStats{mobsKilled=%d, isAlive=%b, currentHp=%.1f, currentMp=%.1f, status='%s', uptime=%d, deaths=%d}",
                mobsKilled, isAlive, currentHp, currentMp, status, uptime, deaths
        );
    }
}
package com.lineagebot;

public enum ClassId {
    // Human Fighter -> Third Professions
    DUELIST("Duelist"),
    DREADNOUGHT("Dreadnought"),
    PHOENIX_KNIGHT("Phoenix Knight"),
    HELL_KNIGHT("Hell Knight"),

    // Human Mystic -> Third Professions
    ARCHMAGE("Archmage"),
    SOULTAKER("Soultaker"),
    ARCANA_LORD("Arcana Lord"),
    CARDINAL("Cardinal"),
    HIEROPHANT("Hierophant"),

    // Elf Fighter -> Third Professions
    EVAS_TEMPLAR("Eva's Templar"),
    SWORD_MUSE("Sword Muse"),
    WIND_RIDER("Wind Rider"),
    MOONLIGHT_SENTINEL("Moonlight Sentinel"),

    // Elf Mystic -> Third Professions
    MYSTIC_MUSE("Mystic Muse"),
    ELEMENTAL_MASTER("Elemental Master"),

    // Dark Elf Fighter -> Third Professions
    SHILLIEN_TEMPLAR("Shillien Templar"),
    SPECTRAL_DANCER("Spectral Dancer"),
    GHOST_HUNTER("Ghost Hunter"),
    GHOST_SENTINEL("Ghost Sentinel"),

    // Dark Elf Mystic -> Third Professions
    STORM_SCREAMER("Storm Screamer"),
    SPECTRAL_MASTER("Spectral Master"),

    // Orc Fighter -> Third Professions
    TITAN("Titan"),
    GRAND_KHAVATARI("Grand Khavatari"),

    // Orc Mystic -> Third Professions
    DOMINATOR("Dominator"),
    DOOMCRYER("Doomcryer"),

    // Dwarf Fighter -> Third Professions
    FORTUNE_SEEKER("Fortune Seeker"),
    MAESTRO("Maestro");

    private final String displayName;

    ClassId(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ClassId[] getPlayableClasses() {
        return new ClassId[] {
                DUELIST, DREADNOUGHT, PHOENIX_KNIGHT, HELL_KNIGHT,
                ARCHMAGE, SOULTAKER, ARCANA_LORD, CARDINAL, HIEROPHANT,
                EVAS_TEMPLAR, SWORD_MUSE, WIND_RIDER, MOONLIGHT_SENTINEL,
                MYSTIC_MUSE, ELEMENTAL_MASTER,
                SHILLIEN_TEMPLAR, SPECTRAL_DANCER, GHOST_HUNTER, GHOST_SENTINEL,
                STORM_SCREAMER, SPECTRAL_MASTER,
                TITAN, GRAND_KHAVATARI,
                DOMINATOR, DOOMCRYER,
                FORTUNE_SEEKER, MAESTRO
        };
    }
}
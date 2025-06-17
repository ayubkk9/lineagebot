package com.lineagebot;

public enum ClassId {
    DARK_AVENGER("Dark Avenger"),
    PALADIN("Paladin"),
    GLADIATOR("Gladiator"),
    SPELLSINGER("Spellsinger"),
    SPELLHOWLER("Spellhowler"),
    NECROMANCER("Necromancer"),
    BISHOP("Bishop"),
    PROPHET("Prophet"),
    SWORD_SINGER("Sword Singer"),
    TEMPLE_KNIGHT("Temple Knight"),
    SHILLIEN_KNIGHT("Shillien Knight"),
    BLADEDANCER("Bladedancer"),
    HAWKEYE("Hawkeye"),
    SILVER_RANGER("Silver Ranger"),
    PHANTOM_RANGER("Phantom Ranger"),
    ABYSS_WALKER("Abyss Walker"),
    PLAINS_WALKER("Plains Walker"),
    TREASURE_HUNTER("Treasure Hunter"),
    WARLOCK("Warlock"),
    SORCERER("Sorcerer"),
    WARCRYER("Warcryer"),
    OVERLORD("Overlord"),
    DESTROYER("Destroyer"),
    TYRANT("Tyrant"),
    BOUNTY_HUNTER("Bounty Hunter"),
    WARSMITH("Warsmith");

    private final String displayName;

    ClassId(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ClassId[] getPlayableClasses() {
        return new ClassId[] {
                DARK_AVENGER, PALADIN, GLADIATOR,
                SPELLSINGER, SPELLHOWLER, NECROMANCER,
                BISHOP, PROPHET, SWORD_SINGER,
                TEMPLE_KNIGHT, SHILLIEN_KNIGHT, BLADEDANCER,
                HAWKEYE, SILVER_RANGER, PHANTOM_RANGER,
                ABYSS_WALKER, PLAINS_WALKER, TREASURE_HUNTER,
                WARLOCK, SORCERER, WARCRYER,
                OVERLORD, DESTROYER, TYRANT,
                BOUNTY_HUNTER, WARSMITH
        };
    }
}
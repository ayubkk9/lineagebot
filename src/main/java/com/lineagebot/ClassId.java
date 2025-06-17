package com.lineagebot;

public enum ClassId {
    HUMAN_FIGHTER("Human Fighter"),
    WARRIOR("Warrior"),
    GLADIATOR("Gladiator"),
    ROGUE("Rogue"),
    PALADIN("Paladin"),
    DARK_AVENGER("Dark Avenger"),
    TREASURE_HUNTER("Treasure Hunter"),
    HAWKEYE("Hawkeye"),
    HUMAN_MYSTIC("Human Mystic"),
    WIZARD("Wizard"),
    SORCERER("Sorcerer"),
    NECROMANCER("Necromancer"),
    WARLOCK("Warlock"),
    CLERIC("Cleric"),
    PROPHET("Prophet"),
    BISHOP("Bishop"),
    ELVEN_FIGHTER("Elven Fighter"),
    ELVEN_KNIGHT("Elven Knight"),
    TEMPLE_KNIGHT("Temple Knight"),
    SWORD_SINGER("Sword Singer"),
    ELVEN_SCOUT("Elven Scout"),
    PLAINS_WALKER("Plains Walker"),
    SILVER_RANGER("Silver Ranger"),
    ELVEN_MYSTIC("Elven Mystic"),
    ELVEN_WIZARD("Elven Wizard"),
    ELEMENTAL_SUMMONER("Elemental Summoner"),
    SPELLSINGER("Spellsinger"),
    DARK_FIGHTER("Dark Fighter"),
    PALUS_KNIGHT("Palus Knight"),
    SHILLIEN_KNIGHT("Shillien Knight"),
    BLADEDANCER("Bladedancer"),
    ASSASSIN("Assassin"),
    ABYSS_WALKER("Abyss Walker"),
    PHANTOM_RANGER("Phantom Ranger"),
    DARK_MYSTIC("Dark Mystic"),
    DARK_WIZARD("Dark Wizard"),
    PHANTOM_SUMMONER("Phantom Summoner"),
    SPELLHOWLER("Spellhowler"),
    ORC_FIGHTER("Orc Fighter"),
    ORC_RAIDER("Orc Raider"),
    DESTROYER("Destroyer"),
    MONK("Monk"),
    TYRANT("Tyrant"),
    ORC_MYSTIC("Orc Mystic"),
    ORC_SHAMAN("Orc Shaman"),
    OVERLORD("Overlord"),
    WARCRYER("Warcryer"),
    DWARVEN_FIGHTER("Dwarven Fighter"),
    SCAVENGER("Scavenger"),
    BOUNTY_HUNTER("Bounty Hunter"),
    ARTISAN("Artisan"),
    WARSMITH("Warsmith"),
    DUMMY1("Dummy 1"),
    DUMMY2("Dummy 2"),
    DUMMY3("Dummy 3"),
    DUMMY4("Dummy 4"),
    DUMMY5("Dummy 5"),
    DUMMY6("Dummy 6"),
    DUMMY7("Dummy 7"),
    DUMMY8("Dummy 8"),
    DUMMY9("Dummy 9"),
    DUMMY10("Dummy 10"),
    MALE_SOLDIER("Male Soldier"),
    FEMALE_SOLDIER("Female Soldier"),
    TROOPER("Trooper"),
    WARDER("Warder"),
    BERSERKER("Berserker"),
    MALE_SOULBREAKER("Male Soulbreaker"),
    FEMALE_SOULBREAKER("Female Soulbreaker"),
    ARBALESTER("Arbalester"),
    DOOMBRINGER("Doombringer"),
    MALE_SOULHOUND("Male Soulhound"),
    FEMALE_SOULHOUND("Female Soulhound"),
    TRICKSTER("Trickster"),
    INSPECTOR("Inspector"),
    JUDICATOR("Judicator"),
    UNKNOWN("Unknown");

    private final String displayName;

    ClassId(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ClassId[] getPlayableClasses() {
        return new ClassId[] {
                // Список основных игровых классов
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
package com.lineagebot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillList {
    private static final Map<ClassId, List<Skill>> CLASS_SKILLS = new HashMap<>();

    static {
        // Human Fighter Classes
        CLASS_SKILLS.put(ClassId.DUELIST, new ArrayList<>(Arrays.asList(
                // Gladiator (2nd Profession)
                new Skill("Triple Slash"),
                new Skill("Sonic Focus"),
                new Skill("War Cry"),
                new Skill("Double Sonic Slash"),
                new Skill("Sonic Blaster"),
                new Skill("Sonic Move"),
                new Skill("Lionheart"),
                new Skill("Sonic Force"),
                new Skill("Dual Weapon Mastery"),
                // Duelist (3rd Profession)
                new Skill("Sonic Storm"),
                new Skill("Sonic Rage"),
                new Skill("Sonic Buster"),
                new Skill("War Frenzy"),
                new Skill("Triple Sonic Slash"),
                new Skill("Maximum Sonic Focus"),
                new Skill("Earthquake"),
                new Skill("Rush Impact"),
                new Skill("Critical Sense")
        )));
        CLASS_SKILLS.put(ClassId.DREADNOUGHT, new ArrayList<>(Arrays.asList(
                // Warlord (2nd Profession)
                new Skill("Power Thrust"),
                new Skill("Battle Roar"),
                new Skill("Vicious Stance"),
                new Skill("Thunder Storm"),
                new Skill("Provoke"),
                new Skill("Whirlwind"),
                new Skill("Detect Animal Weakness"),
                new Skill("Fell Swoop"),
                new Skill("Polearm Accuracy"),
                // Dreadnought (3rd Profession)
                new Skill("Shock Blast"),
                new Skill("Thrash"),
                new Skill("Shock Stomp"),
                new Skill("Dread Pool"),
                new Skill("Lance Smash"),
                new Skill("Howl"),
                new Skill("Maximum Provoke"),
                new Skill("Rush"),
                new Skill("Polearm Master")
        )));
        CLASS_SKILLS.put(ClassId.PHOENIX_KNIGHT, new ArrayList<>(Arrays.asList(
                // Paladin (2nd Profession)
                new Skill("Holy Strike"),
                new Skill("Heal"),
                new Skill("Might"),
                new Skill("Holy Armor"),
                new Skill("Shield Stun"),
                new Skill("Sacrifice"),
                new Skill("Holy Blessing"),
                new Skill("Deflect Arrow"),
                new Skill("Vanguard"),
                // Phoenix Knight (3rd Profession)
                new Skill("Holy Blade"),
                new Skill("Shield Fortress"),
                new Skill("Touch of Life"),
                new Skill("Angel Icon"),
                new Skill("Tribunal"),
                new Skill("Shackle"),
                new Skill("Mass Shackle"),
                new Skill("Holy Aura"),
                new Skill("Shield Mastery")
        )));
        CLASS_SKILLS.put(ClassId.HELL_KNIGHT, new ArrayList<>(Arrays.asList(
                // Dark Avenger (2nd Profession)
                new Skill("Power Strike"),
                new Skill("Drain Health"),
                new Skill("Shield Stun"),
                new Skill("Deflect Arrow"),
                new Skill("Summon Dark Panther"),
                new Skill("Horror"),
                new Skill("Hamstring"),
                new Skill("Reflect Damage"),
                new Skill("Vanguard"),
                // Hell Knight (3rd Profession)
                new Skill("Judgment"),
                new Skill("Shield Slam"),
                new Skill("Hell Scream"),
                new Skill("Insane Crusher"),
                new Skill("Seed of Revenge"),
                new Skill("Summon Cursed Bone"),
                new Skill("Shield Bash"),
                new Skill("Dark Form"),
                new Skill("Shield Mastery")
        )));

        // Human Mystic Classes
        CLASS_SKILLS.put(ClassId.ARCHMAGE, new ArrayList<>(Arrays.asList(
                // Sorcerer (2nd Profession)
                new Skill("Flame Strike"),
                new Skill("Blazing Circle"),
                new Skill("Prominence"),
                new Skill("Mana Regeneration"),
                new Skill("Blaze"),
                new Skill("Sleep"),
                new Skill("Slow"),
                new Skill("Aura Bolt"),
                new Skill("Arcane Power"),
                // Archmage (3rd Profession)
                new Skill("Meteor"),
                new Skill("Aura Flare"),
                new Skill("Blazing Skin"),
                new Skill("Star Fall"),
                new Skill("Arcane Chaos"),
                new Skill("Fire Vortex"),
                new Skill("Elemental Assault"),
                new Skill("Volcanic Master"),
                new Skill("Arcane Shield")
        )));
        CLASS_SKILLS.put(ClassId.SOULTAKER, new ArrayList<>(Arrays.asList(
                // Necromancer (2nd Profession)
                new Skill("Corpse Life Drain"),
                new Skill("Summon Zombie"),
                new Skill("Curse Poison"),
                new Skill("Death Spike"),
                new Skill("Curse Fear"),
                new Skill("Vampiric Claw"),
                new Skill("Silence"),
                new Skill("Anchor"),
                new Skill("Gloom"),
                // Soultaker (3rd Profession)
                new Skill("Mass Curse Fear"),
                new Skill("Summon Cursed Man"),
                new Skill("Curse Gloom"),
                new Skill("Mass Summon Zombie"),
                new Skill("Death Mark"),
                new Skill("Vampiric Mist"),
                new Skill("Dark Vortex"),
                new Skill("Mass Anchor"),
                new Skill("Necrotic Power")
        )));
        CLASS_SKILLS.put(ClassId.ARCANA_LORD, new ArrayList<>(Arrays.asList(
                // Warlock (2nd Profession)
                new Skill("Summon Kat the Cat"),
                new Skill("Transfer Pain"),
                new Skill("Body to Mind"),
                new Skill("Servitor Heal"),
                new Skill("Servitor Recharge"),
                new Skill("Servitor Haste"),
                new Skill("Summon Mew the Cat"),
                new Skill("Servitor Might"),
                new Skill("Summon Binding Cubic"),
                // Arcana Lord (3rd Profession)
                new Skill("Summon Spectral Cat"),
                new Skill("Servitor Empowerment"),
                new Skill("Warrior Servitor"),
                new Skill("Wizard Servitor"),
                new Skill("Assassin Servitor"),
                new Skill("Summon Nightshade"),
                new Skill("Servitor Cure"),
                new Skill("Servitor Blessing"),
                new Skill("Arcana Power")
        )));
        CLASS_SKILLS.put(ClassId.CARDINAL, new ArrayList<>(Arrays.asList(
                // Bishop (2nd Profession)
                new Skill("Heal"),
                new Skill("Resurrection"),
                new Skill("Blessing of Queen"),
                new Skill("Greater Heal"),
                new Skill("Cure Poison"),
                new Skill("Remove Curse"),
                new Skill("Dryad Root"),
                new Skill("Peace"),
                new Skill("Vitality Replenish"),
                // Cardinal (3rd Profession)
                new Skill("Mass Resurrection"),
                new Skill("Bless the Body"),
                new Skill("Balance Life"),
                new Skill("Cleanse"),
                new Skill("Salvation"),
                new Skill("Miracle"),
                new Skill("Mass Cure Poison"),
                new Skill("Divine Protection"),
                new Skill("Holy Resistance")
        )));
        CLASS_SKILLS.put(ClassId.HIEROPHANT, new ArrayList<>(Arrays.asList(
                // Prophet (2nd Profession)
                new Skill("Blessing of Might"),
                new Skill("Heal"),
                new Skill("Wind Walk"),
                new Skill("Resist Shock"),
                new Skill("Berserker Spirit"),
                new Skill("Bless Shield"),
                new Skill("Magic Barrier"),
                new Skill("Haste"),
                new Skill("Guidance"),
                // Hierophant (3rd Profession)
                new Skill("Prophecy of Fire"),
                new Skill("Invigor"),
                new Skill("Prophecy of Wind"),
                new Skill("Prophecy of Water"),
                new Skill("Block Shield"),
                new Skill("Block Wind Walk"),
                new Skill("Greater Might"),
                new Skill("Greater Shield"),
                new Skill("Mystic Immunity")
        )));

        // Elf Fighter Classes
        CLASS_SKILLS.put(ClassId.EVAS_TEMPLAR, new ArrayList<>(Arrays.asList(
                // Temple Knight (2nd Profession)
                new Skill("Shield Bash"),
                new Skill("Summon Storm Cubic"),
                new Skill("Deflect Arrow"),
                new Skill("Holy Armor"),
                new Skill("Guard Stance"),
                new Skill("Aegis Stance"),
                new Skill("Summon Life Cubic"),
                new Skill("Hate"),
                new Skill("Vanguard"),
                // Eva's Templar (3rd Profession)
                new Skill("Holy Blade"),
                new Skill("Shield Fortress"),
                new Skill("Touch of Eva"),
                new Skill("Tribunal"),
                new Skill("Mass Shackle"),
                new Skill("Summon Attractive Cubic"),
                new Skill("Holy Aura"),
                new Skill("Angel Icon"),
                new Skill("Shield Mastery")
        )));
        CLASS_SKILLS.put(ClassId.SWORD_MUSE, new ArrayList<>(Arrays.asList(
                // Sword Singer (2nd Profession)
                new Skill("Song of Hunter"),
                new Skill("Sonic Blaster"),
                new Skill("Song of Wind"),
                new Skill("Song of Vitality"),
                new Skill("Song of Warding"),
                new Skill("Song of Meditation"),
                new Skill("Song of Life"),
                new Skill("Song of Earth"),
                new Skill("Song of Water"),
                // Sword Muse (3rd Profession)
                new Skill("Song of Renewal"),
                new Skill("Song of Champion"),
                new Skill("Sonic Storm"),
                new Skill("Song of Flame Guard"),
                new Skill("Song of Storm Guard"),
                new Skill("Song of Vengeance"),
                new Skill("Song of Invocation"),
                new Skill("Siren’s Dance"),
                new Skill("Sword Symphony")
        )));
        CLASS_SKILLS.put(ClassId.WIND_RIDER, new ArrayList<>(Arrays.asList(
                // Plains Walker (2nd Profession)
                new Skill("Backstab"),
                new Skill("Deadly Blow"),
                new Skill("Dash"),
                new Skill("Critical Blow"),
                new Skill("Bleed"),
                new Skill("Lure"),
                new Skill("Dagger Mastery"),
                new Skill("Unlock"),
                new Skill("Evade"),
                // Wind Rider (3rd Profession)
                new Skill("Lethal Blow"),
                new Skill("Sand Bomb"),
                new Skill("Shadow Step"),
                new Skill("Blinding Blow"),
                new Skill("Focus Death"),
                new Skill("Focus Chance"),
                new Skill("Focus Power"),
                new Skill("Mortal Strike"),
                new Skill("Critical Wound")
        )));
        CLASS_SKILLS.put(ClassId.MOONLIGHT_SENTINEL, new ArrayList<>(Arrays.asList(
                // Silver Ranger (2nd Profession)
                new Skill("Double Shot"),
                new Skill("Elemental Shot"),
                new Skill("Rapid Shot"),
                new Skill("Stunning Shot"),
                new Skill("Burst Shot"),
                new Skill("Entangle"),
                new Skill("Spirit of Sagittarius"),
                new Skill("Bow Mastery"),
                new Skill("Snipe"),
                // Moonlight Sentinel (3rd Profession)
                new Skill("Lethal Shot"),
                new Skill("Arrow Rain"),
                new Skill("Guidance"),
                new Skill("Hamstring Shot"),
                new Skill("Pain of Sagittarius"),
                new Skill("Quiver of Arrows"),
                new Skill("Spirit of Archer"),
                new Skill("Flame Hawk"),
                new Skill("Deadly Aim")
        )));

        // Elf Mystic Classes
        CLASS_SKILLS.put(ClassId.MYSTIC_MUSE, new ArrayList<>(Arrays.asList(
                // Spellsinger (2nd Profession)
                new Skill("Ice Bolt"),
                new Skill("Blizzard"),
                new Skill("Mana Regeneration"),
                new Skill("Frost Wall"),
                new Skill("Hydro Blast"),
                new Skill("Sleep"),
                new Skill("Freezing Skin"),
                new Skill("Frost Bolt"),
                new Skill("Arcane Power"),
                // Mystic Muse (3rd Profession)
                new Skill("Elemental Symphony"),
                new Skill("Ice Vortex"),
                new Skill("Diamond Dust"),
                new Skill("Throne of Ice"),
                new Skill("Star Fall"),
                new Skill("Elemental Assault"),
                new Skill("Freezing Flame"),
                new Skill("Arcane Shield"),
                new Skill("Mystic Mastery")
        )));
        CLASS_SKILLS.put(ClassId.ELEMENTAL_MASTER, new ArrayList<>(Arrays.asList(
                // Elemental Summoner (2nd Profession)
                new Skill("Summon Unicorn"),
                new Skill("Servitor Heal"),
                new Skill("Aqua Swirl"),
                new Skill("Transfer Pain"),
                new Skill("Servitor Recharge"),
                new Skill("Servitor Haste"),
                new Skill("Summon Merrow the Unicorn"),
                new Skill("Servitor Might"),
                new Skill("Summon Aqua Cubic"),
                // Elemental Master (3rd Profession)
                new Skill("Summon Seraphim"),
                new Skill("Servitor Empowerment"),
                new Skill("Warrior Servitor"),
                new Skill("Wizard Servitor"),
                new Skill("Assassin Servitor"),
                new Skill("Magnus the Unicorn"),
                new Skill("Servitor Cure"),
                new Skill("Servitor Blessing"),
                new Skill("Arcana Power")
        )));

        // Dark Elf Fighter Classes
        CLASS_SKILLS.put(ClassId.SHILLIEN_TEMPLAR, new ArrayList<>(Arrays.asList(
                // Shillien Knight (2nd Profession)
                new Skill("Drain Health"),
                new Skill("Lightning Strike"),
                new Skill("Shield Stun"),
                new Skill("Hex"),
                new Skill("Summon Vampire Cubic"),
                new Skill("Hate"),
                new Skill("Poison"),
                new Skill("Guard Stance"),
                new Skill("Vanguard"),
                // Shillien Templar (3rd Profession)
                new Skill("Summon Dark Panther"),
                new Skill("Shield Fortress"),
                new Skill("Judgment"),
                new Skill("Tribunal"),
                new Skill("Mass Shackle"),
                new Skill("Summon Attractive Cubic"),
                new Skill("Spirit of Shillen"),
                new Skill("Lightning Shock"),
                new Skill("Shield Mastery")
        )));
        CLASS_SKILLS.put(ClassId.SPECTRAL_DANCER, new ArrayList<>(Arrays.asList(
                // Bladedancer (2nd Profession)
                new Skill("Dance of Fire"),
                new Skill("Sting"),
                new Skill("Dance of Fury"),
                new Skill("Dance of Light"),
                new Skill("Dance of Concentration"),
                new Skill("Dance of Inspiration"),
                new Skill("Dance of Mystic"),
                new Skill("Dance of Shadow"),
                new Skill("Dance of Vampire"),
                // Spectral Dancer (3rd Profession)
                new Skill("Dance of Warrior"),
                new Skill("Dance of Blade Storm"),
                new Skill("Dance of Protection"),
                new Skill("Poison Blade Dance"),
                new Skill("Demonic Blade Dance"),
                new Skill("Dance of Siren"),
                new Skill("Dance of Berserker"),
                new Skill("Shadow Blade"),
                new Skill("Blade Mastery")
        )));
        CLASS_SKILLS.put(ClassId.GHOST_HUNTER, new ArrayList<>(Arrays.asList(
                // Abyss Walker (2nd Profession)
                new Skill("Backstab"),
                new Skill("Deadly Blow"),
                new Skill("Shadow Step"),
                new Skill("Blinding Blow"),
                new Skill("Bleed"),
                new Skill("Lure"),
                new Skill("Dagger Mastery"),
                new Skill("Unlock"),
                new Skill("Evade"),
                // Ghost Hunter (3rd Profession)
                new Skill("Lethal Blow"),
                new Skill("Sand Bomb"),
                new Skill("Focus Death"),
                new Skill("Focus Chance"),
                new Skill("Focus Power"),
                new Skill("Mortal Strike"),
                new Skill("Critical Wound"),
                new Skill("Shadow Hide"),
                new Skill("Ghost Walking")
        )));
        CLASS_SKILLS.put(ClassId.GHOST_SENTINEL, new ArrayList<>(Arrays.asList(
                // Phantom Ranger (2nd Profession)
                new Skill("Double Shot"),
                new Skill("Lethal Shot"),
                new Skill("Dead Eye"),
                new Skill("Stunning Shot"),
                new Skill("Burst Shot"),
                new Skill("Entangle"),
                new Skill("Spirit of Sagittarius"),
                new Skill("Bow Mastery"),
                new Skill("Snipe"),
                // Ghost Sentinel (3rd Profession)
                new Skill("Arrow Rain"),
                new Skill("Hamstring Shot"),
                new Skill("Pain of Sagittarius"),
                new Skill("Quiver of Arrows"),
                new Skill("Spirit of Archer"),
                new Skill("Flame Hawk"),
                new Skill("Seven Arrow"),
                new Skill("Multiple Shot"),
                new Skill("Deadly Aim")
        )));

        // Dark Elf Mystic Classes
        CLASS_SKILLS.put(ClassId.STORM_SCREAMER, new ArrayList<>(Arrays.asList(
                // Spellhowler (2nd Profession)
                new Skill("Wind Strike"),
                new Skill("Hurricane"),
                new Skill("Curse Fear"),
                new Skill("Vampiric Claw"),
                new Skill("Silence"),
                new Skill("Corpse Life Drain"),
                new Skill("Shadow Flare"),
                new Skill("Slow"),
                new Skill("Arcane Power"),
                // Storm Screamer (3rd Profession)
                new Skill("Tempest"),
                new Skill("Curse Gloom"),
                new Skill("Dark Vortex"),
                new Skill("Wind Vortex"),
                new Skill("Demon Wind"),
                new Skill("Elemental Assault"),
                new Skill("Star Fall"),
                new Skill("Empowering Echo"),
                new Skill("Storm Magic")
        )));
        CLASS_SKILLS.put(ClassId.SPECTRAL_MASTER, new ArrayList<>(Arrays.asList(
                // Phantom Summoner (2nd Profession)
                new Skill("Summon Nightshade"),
                new Skill("Servitor Heal"),
                new Skill("Death Spike"),
                new Skill("Transfer Pain"),
                new Skill("Servitor Recharge"),
                new Skill("Servitor Haste"),
                new Skill("Summon Soulless"),
                new Skill("Servitor Might"),
                new Skill("Summon Shadow Cubic"),
                // Spectral Master (3rd Profession)
                new Skill("Summon Spectral Lord"),
                new Skill("Servitor Empowerment"),
                new Skill("Warrior Servitor"),
                new Skill("Wizard Servitor"),
                new Skill("Assassin Servitor"),
                new Skill("Summon Hexed Bone"),
                new Skill("Servitor Cure"),
                new Skill("Servitor Blessing"),
                new Skill("Arcana Mastery")
        )));

        // Orc Fighter Classes
        CLASS_SKILLS.put(ClassId.TITAN, new ArrayList<>(Arrays.asList(
                // Destroyer (2nd Profession)
                new Skill("Power Smash"),
                new Skill("Frenzy"),
                new Skill("Guts"),
                new Skill("Fatal Strike"),
                new Skill("Rage"),
                new Skill("Crush of Doom"),
                new Skill("Zealot"),
                // Titan (3rd Profession)
                new Skill("Demolition"),
                new Skill("Over the Body"),
                new Skill("Ruin"),
                new Skill("Lion’s Roar"),
                new Skill("Earth Shatter"),
                new Skill("Burning Rage"),
                new Skill("Destroyer Impact"),
                new Skill("Frenzied Power"),
                new Skill("Titan Mastery")
        )));
        CLASS_SKILLS.put(ClassId.GRAND_KHAVATARI, new ArrayList<>(Arrays.asList(
                // Tyrant (2nd Profession)
                new Skill("Force Blaster"),
                new Skill("Burning Fist"),
                new Skill("Force Storm"),
                new Skill("Hawk Fist"),
                new Skill("Totem Spirit Wolf"),
                new Skill("Totem Spirit Puma"),
                new Skill("Zealot"),
                // Grand Khavatari (3rd Profession)
                new Skill("Force Burst"),
                new Skill("Bison Force"),
                new Skill("Ogre Spirit"),
                new Skill("Totem Spirit Bear"),
                new Skill("Force Strike"),
                new Skill("Force Tempest"),
                new Skill("Burning Strike"),
                new Skill("Fist of Fury"),
                new Skill("Force Mastery")
        )));

        // Orc Mystic Classes
        CLASS_SKILLS.put(ClassId.DOMINATOR, new ArrayList<>(Arrays.asList(
                // Overlord (2nd Profession)
                new Skill("Seal of Poison"),
                new Skill("Seal of Fire"),
                new Skill("Chant of Victory"),
                new Skill("Seal of Silence"),
                new Skill("Seal of Winter"),
                new Skill("Seal of Flame"),
                new Skill("Seal of Mirage"),
                new Skill("Seal of Scourge"),
                new Skill("Clan Aura"),
                // Dominator (3rd Profession)
                new Skill("Seal of Suspension"),
                new Skill("Seal of Despair"),
                new Skill("Paagrio’s Might"),
                new Skill("Paagrio’s Shield"),
                new Skill("Paagrio’s Glory"),
                new Skill("Paagrio’s Fist"),
                new Skill("Paagrio’s Flame"),
                new Skill("Paagrio’s Honor"),
                new Skill("Dominator’s Authority")
        )));
        CLASS_SKILLS.put(ClassId.DOOMCRYER, new ArrayList<>(Arrays.asList(
                // Warcryer (2nd Profession)
                new Skill("Chant of Fire"),
                new Skill("Chant of Battle"),
                new Skill("Chant of Shield"),
                new Skill("Chant of Flame"),
                new Skill("Chant of Fury"),
                new Skill("Chant of Evasion"),
                new Skill("Chant of Vampire"),
                new Skill("Chant of Spirit"),
                new Skill("Soul Cry"),
                // Doomcryer (3rd Profession)
                new Skill("Chant of Rage"),
                new Skill("Chant of Glory"),
                new Skill("Chant of Berserker"),
                new Skill("Great Fury"),
                new Skill("Chant of Predator"),
                new Skill("Chant of Protection"),
                new Skill("Chant of Earth"),
                new Skill("Chant of Revenge"),
                new Skill("War Cry Mastery")
        )));

        // Dwarf Fighter Classes
        CLASS_SKILLS.put(ClassId.FORTUNE_SEEKER, new ArrayList<>(Arrays.asList(
                // Bounty Hunter (2nd Profession)
                new Skill("Spoil"),
                new Skill("Crushing Strike"),
                new Skill("Sweeper"),
                new Skill("Fake Death"),
                new Skill("Spoil Crush"),
                new Skill("Blunt Mastery"),
                new Skill("Stun Attack"),
                new Skill("Collector’s Fortune"),
                new Skill("Vital Force"),
                // Fortune Seeker (3rd Profession)
                new Skill("Sweeper Festival"),
                new Skill("Blunt Smash"),
                new Skill("Lucky Strike"),
                new Skill("Collector’s Experience"),
                new Skill("Critical Sense"),
                new Skill("Earthquake"),
                new Skill("Spoil Bomb"),
                new Skill("Fortune Mastery")
        )));
        CLASS_SKILLS.put(ClassId.MAESTRO, new ArrayList<>(Arrays.asList(
                // Warsmith (2nd Profession)
                new Skill("Whirlwind"),
                new Skill("Summon Mechanic Golem"),
                new Skill("Repair"),
                new Skill("Hammer Crush"),
                new Skill("Blunt Mastery"),
                new Skill("Summon Wild Hog Cannon"),
                new Skill("Create Item"),
                // Maestro (3rd Profession)
                new Skill("Summon Siege Golem"),
                new Skill("Earthquake"),
                new Skill("Battle Stun"),
                new Skill("Critical Stun"),
                new Skill("Create Rare Item"),
                new Skill("Critical Sense"),
                new Skill("Big Boom"),
                new Skill("Golem Armor"),
                new Skill("Craft Mastery")
        )));
    }

    public static List<Skill> getSkillsForClass(ClassId classId) {
        return CLASS_SKILLS.getOrDefault(classId, new ArrayList<>(Arrays.asList(
                new Skill("Default Attack"),
                new Skill("Default Heal"),
                new Skill("Default Buff"),
                new Skill("Default Skill")
        )));
    }
}
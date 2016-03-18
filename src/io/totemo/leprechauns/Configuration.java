package io.totemo.leprechauns;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.event.entity.EntityEvent;

// ----------------------------------------------------------------------------
/**
 * Reads and exposes the plugin configuration.
 */
public class Configuration {
    public boolean DEBUG_DEATH;
    public boolean DEBUG_REMOVE;
    public boolean DEBUG_SPAWN_NATURAL;

    /**
     * The world where mobs are affected.
     */
    public World WORLD_NAME;

    /**
     * World border radius in the affected world (square world assumed).
     */
    public int WORLD_BORDER;

    /**
     * Leprechaun health.
     */
    public int LEPRECHAUN_HEALTH;

    /**
     * Chance that a leprechaun is a baby.
     */
    public double LEPRECHAUN_BABY_CHANCE;

    /**
     * True if the leprechauns wear coloured leather armour.
     */
    public boolean ARMOUR_WORN;

    /**
     * Base drop chance for armour pieces, modified by looting.
     */
    public float ARMOUR_DROP_CHANCE;

    /**
     * Chance of a pot of gold spwning upon leprechaun death.
     */
    public double POTS_CHANCE;

    /**
     * Maximum number of pots of gold allowed in the world simultaneously.
     */
    public int POTS_MAX;

    /**
     * Minimum distance from leprechaun death for a pot of gold to spawn.
     */
    public int POTS_RANGE_MIN;

    /**
     * Maximum distance from leprechaun death for a pot of gold to spawn.
     */
    public int POTS_RANGE_MAX;

    /**
     * Extra ticks of time given to the player to find the pot.
     */
    public int POTS_EXTRA_TICKS;

    /**
     * The minimum expected movement speed of players, based upon which the
     * duration of existence of a pot of gold will be calculated.
     */
    public double POTS_MIN_PLAYER_SPEED;

    // ------------------------------------------------------------------------
    /**
     * Load the plugin configuration.
     */
    public void reload() {
        Leprechauns.PLUGIN.reloadConfig();

        DEBUG_DEATH = Leprechauns.PLUGIN.getConfig().getBoolean("debug.death");
        DEBUG_REMOVE = Leprechauns.PLUGIN.getConfig().getBoolean("debug.remove");
        DEBUG_SPAWN_NATURAL = Leprechauns.PLUGIN.getConfig().getBoolean("debug.spawn.natural");

        WORLD_NAME = Bukkit.getWorld(Leprechauns.PLUGIN.getConfig().getString("world.name"));
        if (WORLD_NAME == null) {
            WORLD_NAME = Bukkit.getWorld("world");
        }
        WORLD_BORDER = Leprechauns.PLUGIN.getConfig().getInt("world.border");
        LEPRECHAUN_HEALTH = Leprechauns.PLUGIN.getConfig().getInt("leprechaun.health");
        LEPRECHAUN_BABY_CHANCE = Leprechauns.PLUGIN.getConfig().getDouble("leprechaun.baby_chance");
        ARMOUR_WORN = Leprechauns.PLUGIN.getConfig().getBoolean("armour.worn");
        ARMOUR_DROP_CHANCE = (float) Leprechauns.PLUGIN.getConfig().getDouble("armour.drop_chance");
        POTS_CHANCE = Leprechauns.PLUGIN.getConfig().getDouble("pots.chance");
        POTS_MAX = Leprechauns.PLUGIN.getConfig().getInt("pots.max");
        POTS_RANGE_MIN = Leprechauns.PLUGIN.getConfig().getInt("pots.range.min");
        POTS_RANGE_MAX = Leprechauns.PLUGIN.getConfig().getInt("pots.range.max");
        POTS_EXTRA_TICKS = Leprechauns.PLUGIN.getConfig().getInt("pots.extra_ticks");
        POTS_MIN_PLAYER_SPEED = Leprechauns.PLUGIN.getConfig().getDouble("pots.min_player_speed");
    } // reload

    // ------------------------------------------------------------------------
    /**
     * Return true if the world of the entity event is the configured affected
     * world.
     *
     * @param event an entity-related event.
     * @return true if the world of the entity event is the configured affected
     *         world.
     */
    public boolean isAffectedWorld(EntityEvent event) {
        return isAffectedWorld(event.getEntity().getLocation().getWorld());
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified world is the configured affected world.
     *
     * @param world the world to check.
     * @return true if the specified world is the configured affected world.
     */
    public boolean isAffectedWorld(World world) {
        return world.equals(WORLD_NAME);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random leprechaun name.
     *
     * @return a random leprechaun name.
     */
    @SuppressWarnings("unchecked")
    public String randomLeprechaunName() {
        List<String> firstNames = (List<String>) Leprechauns.PLUGIN.getConfig().getList("messages.names.first");
        List<String> surnames = (List<String>) Leprechauns.PLUGIN.getConfig().getList("messages.names.surname");
        return ChatColor.translateAlternateColorCodes('&',
            Leprechauns.PLUGIN.getConfig().getString("messages.names.colour") +
            firstNames.get(_random.nextInt(firstNames.size())) + " " +
            surnames.get(_random.nextInt(surnames.size())));
    }

    // ------------------------------------------------------------------------
    /**
     * Load the specified key from the configuration, split it into parts,
     * separated by '|', and translate alternate colour codes in the parts to
     * make a list of lore strings.
     *
     * @param key the config key.
     * @return a list of lore strings.
     */
    protected ArrayList<String> loadAndTranslateLore(String key) {
        ArrayList<String> loreList = new ArrayList<String>();
        for (String lore : Leprechauns.PLUGIN.getConfig().getString(key, "").split("\\|")) {
            loreList.add(ChatColor.translateAlternateColorCodes('&', lore));
        }
        return loreList;
    }

    // ------------------------------------------------------------------------
    /**
     * Random number generator.
     */
    protected Random _random = new Random();
} // class Configuration
package io.totemo.leprechauns;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.plugin.Plugin;

// ----------------------------------------------------------------------------
/**
 * Reads and exposes the plugin configuration.
 */
public class Configuration {
    /**
     * If true, log configuration when loaded.
     */
    public boolean DEBUG_CONFIG;

    /**
     * If true, log special mob deaths.
     */
    public boolean DEBUG_DEATH;

    /**
     * If true, log special mob spawns.
     */
    public boolean DEBUG_SPAWN_NATURAL;

    /**
     * The world where mobs are affected.
     */
    public World WORLD;

    /**
     * World border radius in the affected world (square world assumed).
     */
    public int WORLD_BORDER;

    /**
     * If true, leprechauns can despawn when the player is far away, even though
     * they have a custom name.
     */
    public boolean LEPRECHAUN_CAN_DESPAWN;

    /**
     * Chance, [0.0,1.0] that a mob in the affected world will be replaced by a
     * leprechaun.
     */
    public double LEPRECHAUN_CHANCE;

    /**
     * Leprechaun health.
     */
    public int LEPRECHAUN_HEALTH;

    /**
     * Chance that a leprechaun is a baby.
     */
    public double LEPRECHAUN_BABY_CHANCE;

    /**
     * Chance that a leprechaun is a villager.
     */
    public double LEPRECHAUN_VILLAGER_CHANCE;

    /**
     * Chance that a leprechaun will drop its weapon.
     */
    public double LEPRECHAUN_WEAPON_DROP_CHANCE;

    /**
     * True if the leprechauns wear coloured leather armour.
     */
    public boolean LEPRECHAUN_ARMOUR_WORN;

    /**
     * Base drop chance for armour pieces, modified by looting.
     */
    public float LEPRECHAUN_ARMOUR_DROP_CHANCE;

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

    /**
     * Radius of the particle cloud around the objective.
     */
    public float POTS_PARTICLE_RADIUS;

    /**
     * Number of particles in the particle cloud around the objective.
     */
    public int POTS_PARTICLE_COUNT;

    /**
     * Item name on maps to objectives.
     */
    public String POTS_MAP_NAME;

    /**
     * Lore on maps to objectives.
     * 
     * By convention, the String must be split at the pipe character, after
     * message substitution of {0} as the objective coordinates.
     */
    public String POTS_MAP_LORE;

    /**
     * Regular drops.
     */
    public ArrayList<Drop> DROPS_REGULAR;

    /**
     * Special drops, which drop when a player has damaged the mob in the last 5
     * seconds.
     */
    public ArrayList<Drop> DROPS_SPECIAL;

    /**
     * Objective drops, one of which is randomly selected and dropped when a
     * player reaches an objective within the time limit.
     */
    public ArrayList<Drop> DROPS_POTS;

    // ------------------------------------------------------------------------
    /**
     * Load the plugin configuration.
     */
    public void reload() {
        Plugin plugin = Leprechauns.PLUGIN;
        try {
            plugin.reloadConfig();
        } catch (Exception ex) {
            plugin.getLogger().info("Exception loading configuration: " + ex.getMessage());
            Throwable cause = ex.getCause();
            if (cause != null) {
                plugin.getLogger().info("Because: " + cause.getClass().getSimpleName() + " " + cause.getMessage());
            }
        }

        // Note: this configuration instance replaced by reloadConfig().
        FileConfiguration config = plugin.getConfig();

        DEBUG_CONFIG = config.getBoolean("debug.config");
        DEBUG_DEATH = config.getBoolean("debug.death");
        DEBUG_SPAWN_NATURAL = config.getBoolean("debug.spawn.natural");

        WORLD = Bukkit.getWorld(config.getString("world.name"));
        if (WORLD == null) {
            WORLD = Bukkit.getWorld("world");
        }
        if (WORLD == null) {
            WORLD = Bukkit.getWorlds().get(0);
        }
        WORLD_BORDER = config.getInt("world.border");

        LEPRECHAUN_CAN_DESPAWN = config.getBoolean("leprechaun.can_despawn");
        LEPRECHAUN_CHANCE = config.getDouble("leprechaun.chance");
        LEPRECHAUN_HEALTH = config.getInt("leprechaun.health");
        LEPRECHAUN_BABY_CHANCE = config.getDouble("leprechaun.baby_chance");
        LEPRECHAUN_VILLAGER_CHANCE = config.getDouble("leprechaun.villager_chance");
        LEPRECHAUN_WEAPON_DROP_CHANCE = config.getDouble("leprechaun.weapon_drop_chance");
        LEPRECHAUN_ARMOUR_WORN = config.getBoolean("leprechaun.armour.worn");
        LEPRECHAUN_ARMOUR_DROP_CHANCE = (float) config.getDouble("leprechaun.armour.drop_chance");

        POTS_CHANCE = config.getDouble("pots.chance");
        POTS_MAX = config.getInt("pots.max");
        POTS_RANGE_MIN = config.getInt("pots.range.min");
        POTS_RANGE_MAX = config.getInt("pots.range.max");
        POTS_EXTRA_TICKS = config.getInt("pots.extra_ticks");
        POTS_MIN_PLAYER_SPEED = config.getDouble("pots.min_player_speed");
        POTS_MAP_NAME = ChatColor.translateAlternateColorCodes('&', config.getString("pots.map.name"));
        POTS_MAP_LORE = ChatColor.translateAlternateColorCodes('&', config.getString("pots.map.lore"));
        POTS_PARTICLE_RADIUS = (float) config.getDouble("pots.particle.radius");
        POTS_PARTICLE_COUNT = config.getInt("pots.particle.count");

        DROPS_REGULAR = loadDrops(config.getConfigurationSection("drops.regular"));
        DROPS_SPECIAL = loadDrops(config.getConfigurationSection("drops.special"));
        DROPS_POTS = loadDrops(config.getConfigurationSection("drops.pots"));

        if (DEBUG_CONFIG) {
            Logger logger = plugin.getLogger();
            logger.info("Configuration:");
            logger.info("DEBUG_DEATH: " + DEBUG_DEATH);
            logger.info("DEBUG_NATURAL_SPAWN: " + DEBUG_SPAWN_NATURAL);

            logger.info("WORLD: " + WORLD.getName());
            logger.info("WORLD_BORDER: " + WORLD_BORDER);

            logger.info("LEPRECHAUN_CAN_DESPAWN: " + LEPRECHAUN_CAN_DESPAWN);
            logger.info("LEPRECHAUN_CHANCE: " + LEPRECHAUN_CHANCE);
            logger.info("LEPRECHAUN_HEALTH: " + LEPRECHAUN_HEALTH);
            logger.info("LEPRECHAUN_BABY_CHANCE: " + LEPRECHAUN_BABY_CHANCE);
            logger.info("LEPRECHAUN_VILLAGER_CHANCE: " + LEPRECHAUN_VILLAGER_CHANCE);
            logger.info("LEPRECHAUN_WEAPON_DROP_CHANCE: " + LEPRECHAUN_WEAPON_DROP_CHANCE);

            logger.info("LEPRECHAUN_ARMOUR_WORN: " + LEPRECHAUN_ARMOUR_WORN);
            logger.info("LEPRECHAUN_ARMOUR_DROP_CHANCE: " + LEPRECHAUN_ARMOUR_DROP_CHANCE);

            logger.info("POTS_CHANCE: " + POTS_CHANCE);
            logger.info("POTS_MAX: " + POTS_MAX);
            logger.info("POTS_RANGE_MIN: " + POTS_RANGE_MIN);
            logger.info("POTS_RANGE_MAX: " + POTS_RANGE_MAX);
            logger.info("POTS_EXTRA_TICKS: " + POTS_EXTRA_TICKS);
            logger.info("POTS_MIN_PLAYER_SPEED: " + POTS_MIN_PLAYER_SPEED);

            logger.info("POTS_MAP_NAME: " + POTS_MAP_NAME);
            logger.info("POTS_MAP_LORE: " + POTS_MAP_LORE);
            logger.info("POTS_PARTICLE_RADIUS: " + POTS_PARTICLE_RADIUS);
            logger.info("POTS_PARTICLE_COUNT: " + POTS_PARTICLE_COUNT);

            logger.info("DROPS_REGULAR:");
            for (Drop drop : DROPS_REGULAR) {
                logger.info(drop.toString());
            }
            logger.info("DROPS_SPECIAL:");
            for (Drop drop : DROPS_SPECIAL) {
                logger.info(drop.toString());
            }
            logger.info("DROPS_POTS:");
            for (Drop drop : DROPS_POTS) {
                logger.info(drop.toString());
            }

            List<String> firstNames = (List<String>) config.getList("messages.names.first");
            List<String> lastNames = (List<String>) config.getList("messages.names.surname");

            logger.info("First names: " + firstNames.stream().collect(Collectors.joining(" ")));
            logger.info("Last names: " + lastNames.stream().collect(Collectors.joining(" ")));
        }

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
        return world.equals(WORLD);
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
        return ChatColor.translateAlternateColorCodes('&', Leprechauns.PLUGIN.getConfig().getString("messages.names.colour") +
                                                           firstNames.get(Util.randomInt(firstNames.size())) + " " +
                                                           surnames.get(Util.randomInt(surnames.size())));
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
     * Load an array of {@link Drop}s from the specified section,
     *
     * @param section the configuration section.
     * @return the array of {@link Drop}s.
     */
    protected ArrayList<Drop> loadDrops(ConfigurationSection section) {
        ArrayList<Drop> drops = new ArrayList<Drop>();
        for (String key : section.getKeys(false)) {
            drops.add(new Drop(section.getConfigurationSection(key)));
        }
        return drops;
    }
} // class Configuration

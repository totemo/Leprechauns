package io.totemo.leprechauns;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

// ----------------------------------------------------------------------------
/**
 * Leprechauns plugin, command handling and event handler.
 *
 * Leprechauns turns hostile mobs into leprechauns.
 */
public class Leprechauns extends JavaPlugin implements Listener {
    /**
     * Configuration wrapper instance.
     */
    public static Configuration CONFIG = new Configuration();

    /**
     * This plugin, accessible as, effectively, a singleton.
     */
    public static Leprechauns PLUGIN;

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        PLUGIN = this;
        LEPRECHAUN_META = new FixedMetadataValue(this, "Leprechaun");

        saveDefaultConfig();
        CONFIG.reload();

        getServer().getPluginManager().registerEvents(this, this);

        // Every tick, do particle effects for the pots of gold.
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                _potManager.tickAll();
            }
        }, 1, 1);
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        _potManager.removeAll();
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase(getName())) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                CONFIG.reload();
                sender.sendMessage(ChatColor.GOLD + getName() + " configuration reloaded.");
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Invalid command syntax.");
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * In the configured World, replace all hostile natural spawns with zombies,
     * dressed as leprechauns.
     *
     * Spawner-spawned mobs are not affected in any way.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!CONFIG.isAffectedWorld(event)) {
            return;
        }

        if (event.getSpawnReason() == SpawnReason.NATURAL &&
            isEligibleHostileMob(event.getEntityType()) &&
            Math.random() < CONFIG.LEPRECHAUN_CHANCE) {

            Entity originalMob = event.getEntity();
            Location loc = originalMob.getLocation();
            originalMob.remove();
            spawnLeprechaun(loc);

            if (CONFIG.DEBUG_SPAWN_NATURAL) {
                getLogger().info("Spawned leprechaun at " + Util.formatLocation(loc));
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If a player breaks a pot of gold, do treasure drops and stop that pot's
     * particle effects.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        if (!CONFIG.isAffectedWorld(loc.getWorld())) {
            return;
        }

        PotOfGold pot = _potManager.getPotOfGold(block);
        if (pot != null) {
            // Prevent the cauldron break from being logged by LogBlock.
            event.setCancelled(true);

            pot.vaporise();
            pot.spawnLoot(event.getPlayer());
            _potManager.removePot(pot);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Tag special mobs hurt by players.
     *
     * Only those mobs hurt recently by players will have special drops.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!CONFIG.isAffectedWorld(event)) {
            return;
        }

        Entity victim = event.getEntity();
        if (victim instanceof LivingEntity && !(victim instanceof ArmorStand)) {
            Location loc = victim.getLocation();
            // Blood particles.
            loc.getWorld().spawnParticle(Particle.BLOCK_DUST, loc, 20, 0.5f, 0.5f, 0.5f, Material.NETHER_WART_BLOCK.createBlockData());
        }

        if (isLeprechaun(victim)) {
            boolean isPlayerAttack = false;
            String playerName = "";
            if (event.getDamager() instanceof Player) {
                isPlayerAttack = true;
                Player player = (Player) event.getDamager();
                playerName = player.getName();
            } else if (event.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) event.getDamager();
                if (projectile.getShooter() instanceof Player) {
                    isPlayerAttack = true;
                }
            }

            // Tag mobs hurt by players with the damage time stamp.
            if (isPlayerAttack) {
                victim.setMetadata(PLAYER_NAME_KEY, new FixedMetadataValue(this, playerName));
                victim.setMetadata(PLAYER_DAMAGE_TIME_KEY, new FixedMetadataValue(this, new Long(victim.getWorld().getFullTime())));
            }
        }
    } // onEntityDamageByEntity

    // ------------------------------------------------------------------------
    /**
     * On mob death, do special drops if a player hurt the mob recently.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!CONFIG.isAffectedWorld(event)) {
            return;
        }

        Entity entity = event.getEntity();
        if (isLeprechaun(entity)) {
            if (CONFIG.DEBUG_DEATH) {
                getLogger().info("Mob died at " + Util.formatLocation(entity.getLocation()));
            }

            boolean specialDrops = false;
            Long damageTime = getPlayerDamageTime(entity);
            if (damageTime != null) {
                Location loc = entity.getLocation();
                if (loc.getWorld().getFullTime() - damageTime < PLAYER_DAMAGE_TICKS) {
                    specialDrops = true;
                }
            }

            // Calculate looting based on what the killing player is
            // holding in his hands at the time the entity dies.
            Player player = Bukkit.getPlayerExact(getPlayerNameMeta(entity));
            if (player == null) {
                return;
            }
            int lootingLevel = Math.max(player.getEquipment().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS),
                                        player.getEquipment().getItemInOffHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS));
            doCustomDrops((Zombie) entity, event.getDrops(), specialDrops, lootingLevel);
        }
    } // onEntityDeath

    // ------------------------------------------------------------------------
    /**
     * Spawn a Leprechaun at the specified location.
     *
     * @param loc the location.
     */
    protected Zombie spawnLeprechaun(Location loc) {
        Zombie leprechaun;
        boolean isVillager = (CONFIG.LEPRECHAUN_VILLAGER_CHANCE < 0.0001) ? false
                                                                          : (Math.random() < CONFIG.LEPRECHAUN_VILLAGER_CHANCE);
        if (isVillager) {
            ZombieVillager zombager = (ZombieVillager) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE_VILLAGER);
            zombager.setVillagerProfession(VILLAGER_PROFESSIONS[Util.randomInt(VILLAGER_PROFESSIONS.length)]);
            leprechaun = zombager;
        } else {
            leprechaun = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
        }

        // Don't allow players to steal the weapon by throwing items.
        leprechaun.setCanPickupItems(false);
        leprechaun.getEquipment().clear();
        leprechaun.setMaxHealth(CONFIG.LEPRECHAUN_HEALTH);
        leprechaun.setHealth(CONFIG.LEPRECHAUN_HEALTH);
        leprechaun.setBaby(Math.random() < CONFIG.LEPRECHAUN_BABY_CHANCE);

        leprechaun.setCustomNameVisible(true);
        leprechaun.setCustomName(CONFIG.randomLeprechaunName());
        leprechaun.setRemoveWhenFarAway(CONFIG.LEPRECHAUN_CAN_DESPAWN);
        leprechaun.setMetadata(LEPRECHAUN_KEY, LEPRECHAUN_META);
        leprechaun.getEquipment().setItemInMainHand(makeCustomWeapon());
        leprechaun.getEquipment().setItemInMainHandDropChance((float) CONFIG.LEPRECHAUN_WEAPON_DROP_CHANCE);

        if (CONFIG.LEPRECHAUN_ARMOUR_WORN) {
            ItemStack helmet = new ItemStack(Material.LEATHER_HELMET, 1, (short) Util.random(1, 30));
            ItemStack chestPlate = new ItemStack(Material.LEATHER_CHESTPLATE, 1, (short) Util.random(1, 30));
            ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS, 1, (short) Util.random(1, 30));
            ItemStack boots = new ItemStack(Material.LEATHER_BOOTS, 1, (short) Util.random(1, 30));
            leprechaun.getEquipment().setHelmet(dyeLeatherArmour(helmet, Color.LIME));
            leprechaun.getEquipment().setChestplate(dyeLeatherArmour(chestPlate, Color.LIME));
            leprechaun.getEquipment().setLeggings(dyeLeatherArmour(leggings, Color.LIME));
            leprechaun.getEquipment().setBoots(dyeLeatherArmour(boots, Color.BLACK));
            leprechaun.getEquipment().setHelmetDropChance(CONFIG.LEPRECHAUN_ARMOUR_DROP_CHANCE);
            leprechaun.getEquipment().setChestplateDropChance(CONFIG.LEPRECHAUN_ARMOUR_DROP_CHANCE);
            leprechaun.getEquipment().setLeggingsDropChance(CONFIG.LEPRECHAUN_ARMOUR_DROP_CHANCE);
            leprechaun.getEquipment().setBootsDropChance(CONFIG.LEPRECHAUN_ARMOUR_DROP_CHANCE);
        }
        return leprechaun;
    } // spawnLeprechaun

    // ------------------------------------------------------------------------
    /**
     * Create a custom enchanted shamrock or shillelagh.
     *
     * @return the ItemStack of the weapon.
     */
    protected ItemStack makeCustomWeapon() {
        ItemStack weapon;
        if (Math.random() < 0.2) {
            weapon = new ItemStack(Material.GRASS, 1);
            ItemMeta meta = weapon.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&2a shamrock"));
            weapon.setItemMeta(meta);

            if (Math.random() < 0.25) {
                weapon.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, Util.random(1, 2));
            }
            if (Math.random() < 0.25) {
                weapon.addUnsafeEnchantment(Enchantment.LUCK, Util.random(1, 4));
            }
            if (Math.random() < 0.25) {
                weapon.addUnsafeEnchantment(Enchantment.LOOT_BONUS_MOBS, Util.random(1, 4));
            }
        } else {
            weapon = new ItemStack(Material.STICK, 1);
            ItemMeta meta = weapon.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&2a shillelagh"));
            weapon.setItemMeta(meta);

            if (Math.random() < 0.25) {
                weapon.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, Util.random(1, 2));
            }
            if (Math.random() < 0.25) {
                weapon.addUnsafeEnchantment(Enchantment.KNOCKBACK, Util.random(1, 4));
            }
            if (Math.random() < 0.25) {
                weapon.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, Util.random(1, 3));
            }
        }
        return weapon;
    } // makeCustomWeapon

    // ------------------------------------------------------------------------
    /**
     * Return the name of the player who killed a mob from metadata on the mob.
     *
     * This metadata is added when a player damages a mob. It is the level of
     * the Looting enchant on the weapon that did the damage, or 0 if there was
     * no such enchant.
     *
     * @param entity the damaged entity.
     * @return the name of the player who killed a mob from metadata on the mob.
     */
    protected String getPlayerNameMeta(Entity entity) {
        List<MetadataValue> name = entity.getMetadata(PLAYER_NAME_KEY);
        return (name.size() > 0) ? name.get(0).asString() : "";
    }

    // ------------------------------------------------------------------------
    /**
     * Return the world time when a player damaged the specified entity, if
     * stored as a PLAYER_DAMAGE_TIME_KEY metadata value, or null if that didn't
     * happen.
     *
     * @param entity the entity (mob).
     * @return the damage time stamp as Long, or null.
     */
    protected Long getPlayerDamageTime(Entity entity) {
        List<MetadataValue> playerDamageTime = entity.getMetadata(PLAYER_DAMAGE_TIME_KEY);
        if (playerDamageTime.size() > 0) {
            MetadataValue value = playerDamageTime.get(0);
            if (value.value() instanceof Long) {
                return (Long) value.value();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Clear the default drops and add in custom ones.
     *
     * @param leprechaun the dropping mob.
     * @param drops the list of drops for the EntityDeathEvent.
     * @param special if true, low-probability, special drops are possible;
     *        otherwise, the drops are custom but mundane.
     * @param lootingLevel the level of looting on the weapon ([0,3]).
     */
    protected void doCustomDrops(Zombie leprechaun, List<ItemStack> drops, boolean special, int lootingLevel) {
        drops.clear();
        for (Drop drop : CONFIG.DROPS_REGULAR) {
            if (drop.isValid() && Math.random() < drop.getDropChance() * adjustedChance(lootingLevel)) {
                drops.add(drop.generate());
            }
        }

        if (special) {
            for (Drop drop : CONFIG.DROPS_SPECIAL) {
                if (drop.isValid() && Math.random() < drop.getDropChance() * adjustedChance(lootingLevel)) {
                    drops.add(drop.generate());
                }
            }

            if (Math.random() < leprechaun.getEquipment().getItemInMainHandDropChance() * adjustedChance(lootingLevel)) {
                drops.add(leprechaun.getEquipment().getItemInMainHand());
            }

            // Spawn a pot of gold?
            if (Math.random() < CONFIG.POTS_CHANCE) {
                PotOfGold pot = _potManager.spawnPotOfGold(leprechaun.getLocation());
                if (pot != null) {
                    getLogger().info("Spawned pot of gold at " + Util.formatLocation(pot.getLocation()) +
                                     " alive for " + pot.getLifeInTicks() + " ticks.");
                    ItemStack map = new ItemStack(Material.PAPER, 1);
                    ItemMeta meta = map.getItemMeta();
                    String formattedLoc = pot.getLocation().getBlockX() + ", " +
                                          pot.getLocation().getBlockY() + ", " +
                                          pot.getLocation().getBlockZ();
                    meta.setDisplayName(MessageFormat.format(CONFIG.POTS_MAP_NAME, formattedLoc));
                    meta.setLore(Arrays.asList(MessageFormat.format(CONFIG.POTS_MAP_LORE, formattedLoc).split("\\|")));
                    map.setItemMeta(meta);
                    drops.add(map);
                }
            }
        }
    } // doCustomDrops

    // ------------------------------------------------------------------------
    /**
     * Return multiplicative factor to apply to the base drop chance according
     * to a given looting level.
     *
     * The drop chance compounds by 20% per looting level.
     *
     * @param lootingLevel the looting level of the weapon.
     * @return a factor to be multiplied by the base drop chance to compute the
     *         actual drop chance.
     */
    protected double adjustedChance(int lootingLevel) {
        return Math.pow(1.2, lootingLevel);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified entity type is that of a hostile mob that is
     * eligible to be replaced with a leprechaun.
     *
     * @param type the entity's type.
     * @return true if the specified entity type is that of a hostile mob that
     *         is eligible to be replaced with a leprechaun.
     */
    protected boolean isEligibleHostileMob(EntityType type) {
        return type == EntityType.CREEPER ||
               type == EntityType.SPIDER ||
               type == EntityType.SKELETON ||
               type == EntityType.ZOMBIE ||
               // TODO: config option of whether zombie villagers spawn (and not
               // replaced).
               type == EntityType.ZOMBIE_VILLAGER ||
               type == EntityType.HUSK ||
               type == EntityType.STRAY ||
               type == EntityType.ENDERMAN ||
               type == EntityType.WITCH;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified mob is a leprechaun.
     *
     * @param mob the mob.
     * @return true if the specified mob is a leprechaun.
     */
    protected boolean isLeprechaun(Entity mob) {
        return mob.hasMetadata(LEPRECHAUN_KEY);
    }

    // ------------------------------------------------------------------------
    /**
     * Dye leather armour.
     *
     * @param armour the armour.
     * @param colour the colour to dye it.
     * @return the armour parameter.
     */
    protected ItemStack dyeLeatherArmour(ItemStack armour, Color colour) {
        LeatherArmorMeta meta = (LeatherArmorMeta) armour.getItemMeta();
        meta.setColor(colour);
        armour.setItemMeta(meta);
        return armour;
    }

    // ------------------------------------------------------------------------
    /**
     * Metadata name (key) used to tag leprechauns.
     */
    protected static final String LEPRECHAUN_KEY = "Lephrechauns_Leprechaun";

    /**
     * Shared metadata value for all disguised entities.
     */
    protected static FixedMetadataValue LEPRECHAUN_META;

    /**
     * Metadata name used for metadata stored on mobs to record last damage time
     * (Long) by a player.
     */
    protected static final String PLAYER_DAMAGE_TIME_KEY = "Leprechauns_PlayerDamageTime";

    /**
     * Metadata name used for metadata stored on mobs to record the name of the
     * player who most recently damaged a mob.
     *
     * This is used to look up the damaging player and compute the looting level
     * at the time of mob death.
     */
    protected static final String PLAYER_NAME_KEY = "Leprechauns_PlayerName";

    /**
     * Time in ticks (1/20ths of a second) for which player attack damage
     * "sticks" to a mob. The time between the last player damage on a mob and
     * its death must be less than this for it to drop special stuff.
     */
    protected static final int PLAYER_DAMAGE_TICKS = 100;

    /**
     * Villager professions, statically computed.
     */
    protected static final Profession[] VILLAGER_PROFESSIONS = Profession.values();

    /**
     * Manages pots of gold.
     */
    protected PotManager _potManager = new PotManager();
} // class Leprechauns

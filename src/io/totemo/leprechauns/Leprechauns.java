package io.totemo.leprechauns;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.darkblade12.particleeffect.ParticleEffect;
import com.google.common.collect.Lists;

// ----------------------------------------------------------------------------
/**
 * Leprechauns plugin, command handling and event handler.
 *
 * Leprechauns turns hostile mobs into leprechauns.
 *
 * The main features are:
 * <ul>
 * <li>The plugin affects hostile mobs in a single configured world (by default,
 * the overworld) only. Spawner mobs are not modified.</li>
 * <li>Affected hostile mobs are replaced with zombies dressed in green leather
 * armour (or skinned with a server resource pack), referred to as leprechauns.</li>
 * <li>For custom mobs to drop special drops when they die, they must have been
 * recently hurt by a player.</li>
 * <li>Leprechauns are armed with shillelaghs (enchanted sticks) and shamrocks
 * (enchanted lily pads), with a small chance of the held item dropping.</li>
 * <li>Leprechauns also drop various potions named as alcoholic beverages, as
 * well as gold nuggets named as coins.</li>
 * <li>Leprechauns have custom Irish names.</li>
 * <li>A small percentage of leprechauns drop a piece of paper named a
 * "Treasure Map", which has as its lore the coordinates of a pot of gold (POG).
 * </li>
 * <li>A POG is a cauldron that is spawned into the affected world (only) for a
 * limited period of time within a range of distances from the leprechaun's
 * death point.</li>
 * <li>The POG is marked with a rainbow of coloured particles. If the player
 * breaks it or moves to within 5 blocks of it, then it disappears and drops
 * configurable loot (typically gold carrots, ingots and nuggets).</li>
 * </ul>
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

        if (event.getSpawnReason() == SpawnReason.NATURAL && isEligibleHostileMob(event.getEntityType())) {
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
     * Tag disguised mobs hurt by players.
     *
     * Only those disguised mobs hurt recently by players will have special
     * drops.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!CONFIG.isAffectedWorld(event)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity.hasMetadata(LEPRECHAUN_KEY)) {
            int lootingLevel = 0;
            boolean isPlayerAttack = false;
            if (event.getDamager() instanceof Player) {
                isPlayerAttack = true;
                Player player = (Player) event.getDamager();
                lootingLevel = player.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
            } else if (event.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) event.getDamager();
                if (projectile.getShooter() instanceof Player) {
                    isPlayerAttack = true;
                }
            }

            // Tag mobs hurt by players with the damage time stamp.
            if (isPlayerAttack) {
                entity.setMetadata(PLAYER_DAMAGE_TIME_KEY, new FixedMetadataValue(this, new Long(entity.getWorld().getFullTime())));
                entity.setMetadata(PLAYER_LOOTING_LEVEL_KEY, new FixedMetadataValue(this, lootingLevel));
            }
        }
    } // onEntityDamageByEntity

    // ------------------------------------------------------------------------
    /**
     * On leprechaun death, do special drops if a player hurt the mob recently.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!CONFIG.isAffectedWorld(event)) {
            return;
        }

        Entity entity = event.getEntity();
        if (isLeprechaun(entity)) {
            handleDeath(entity);

            // Do custom drops.
            int lootingLevel = getLootingLevelMeta(entity);
            boolean specialDrops = false;
            Long damageTime = getPlayerDamageTime(entity);
            if (damageTime != null) {
                Location loc = entity.getLocation();
                if (loc.getWorld().getFullTime() - damageTime < PLAYER_DAMAGE_TICKS) {
                    specialDrops = true;
                }
            }

            doCustomDrops((Zombie) entity, event.getDrops(), specialDrops, lootingLevel);
        }
    } // onEntityDeath

    // ------------------------------------------------------------------------
    /**
     * Spawn a leprechaun at the specified location.
     *
     * @param loc the location.
     */
    protected Zombie spawnLeprechaun(Location loc) {
        Zombie leprechaun = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
        // Don't allow players to steal the weapon by throwing items.
        leprechaun.setCanPickupItems(false);
        leprechaun.getEquipment().clear();
        leprechaun.setMaxHealth(CONFIG.LEPRECHAUN_HEALTH);
        leprechaun.setHealth(CONFIG.LEPRECHAUN_HEALTH);
        leprechaun.setVillager(false);
        if (Math.random() < CONFIG.LEPRECHAUN_BABY_CHANCE) {
            leprechaun.setBaby(true);
        }
        leprechaun.setCustomNameVisible(true);
        leprechaun.setCustomName(CONFIG.randomLeprechaunName());
        leprechaun.setMetadata(LEPRECHAUN_KEY, LEPRECHAUN_META);
        leprechaun.getEquipment().setItemInHand(makeCustomWeapon());
        leprechaun.getEquipment().setItemInHandDropChance(0.16f);

        if (CONFIG.ARMOUR_WORN) {
            ItemStack helmet = new ItemStack(Material.LEATHER_HELMET, 1, (short) Util.random(1, 30));
            ItemStack chestPlate = new ItemStack(Material.LEATHER_CHESTPLATE, 1, (short) Util.random(1, 30));
            ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS, 1, (short) Util.random(1, 30));
            ItemStack boots = new ItemStack(Material.LEATHER_BOOTS, 1, (short) Util.random(1, 30));
            leprechaun.getEquipment().setHelmet(dyeLeatherArmour(helmet, Color.LIME));
            leprechaun.getEquipment().setChestplate(dyeLeatherArmour(chestPlate, Color.LIME));
            leprechaun.getEquipment().setLeggings(dyeLeatherArmour(leggings, Color.LIME));
            leprechaun.getEquipment().setBoots(dyeLeatherArmour(boots, Color.BLACK));
            leprechaun.getEquipment().setHelmetDropChance(CONFIG.ARMOUR_DROP_CHANCE);
            leprechaun.getEquipment().setChestplateDropChance(CONFIG.ARMOUR_DROP_CHANCE);
            leprechaun.getEquipment().setLeggingsDropChance(CONFIG.ARMOUR_DROP_CHANCE);
            leprechaun.getEquipment().setBootsDropChance(CONFIG.ARMOUR_DROP_CHANCE);
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
            weapon = new ItemStack(Material.LONG_GRASS, 1, (short) 1);
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
     * Return the looting level metadata value from a leprechaun.
     *
     * This metadata is added when a player damages a leprechaun. It is the
     * level of the Looting enchant on the weapon that did the damage, or 0 if
     * there was no such enchant.
     *
     * @param entity the damaged entity.
     * @return the level of the Looting enchant, or 0 if not so enchanted.
     */
    protected int getLootingLevelMeta(Entity entity) {
        List<MetadataValue> lootingLevel = entity.getMetadata(PLAYER_LOOTING_LEVEL_KEY);
        if (lootingLevel.size() > 0) {
            return lootingLevel.get(0).asInt();
        }
        return 0;
    }

    // ------------------------------------------------------------------------
    /**
     * Handle the death of a disguised mob by showing death particle effects and
     * removing the disguise.
     *
     * @param mob the mob.
     */
    protected void handleDeath(Entity mob) {
        Location loc = mob.getLocation();
        ParticleEffect.REDSTONE.display(0.3f, 0.3f, 0.3f, 0, 20, loc, 32);

        if (CONFIG.DEBUG_DEATH) {
            getLogger().info("Leprechaun died at " + Util.formatLocation(loc));
        }
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
        if (Math.random() < 0.4 * adjustedChance(lootingLevel)) {
            drops.add(new ItemStack(Material.GOLD_NUGGET, Util.random(1, 4)));
        }

        if (special) {
            if (Math.random() < leprechaun.getEquipment().getItemInHandDropChance() * adjustedChance(lootingLevel)) {
                drops.add(leprechaun.getEquipment().getItemInHand());
            }

            if (Math.random() < 0.1 * adjustedChance(lootingLevel)) {
                drops.add(new ItemStack(Material.GOLD_INGOT, Util.random(1, 2)));
            }

            if (Math.random() < 0.05 * adjustedChance(lootingLevel)) {
                ItemStack potion = new ItemStack(Material.POTION, 1);
                PotionMeta meta = (PotionMeta) potion.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "whiskey");
                meta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, 0), true);
                meta.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 600, 1), true);
                meta.addCustomEffect(new PotionEffect(PotionEffectType.CONFUSION, 1200, 1), true);
                meta.setMainEffect(PotionEffectType.HEAL);
                potion.setItemMeta(meta);
                drops.add(potion);
            }

            if (Math.random() < 0.05 * adjustedChance(lootingLevel)) {
                ItemStack potion = new ItemStack(Material.POTION, 1);
                PotionMeta meta = (PotionMeta) potion.getItemMeta();
                meta.setDisplayName(ChatColor.WHITE + "Guinness");
                meta.addCustomEffect(new PotionEffect(PotionEffectType.CONFUSION, 1200, 1), true);
                potion.setItemMeta(meta);
                drops.add(potion);
            }

            // Spawn a pot of gold?
            if (Math.random() < CONFIG.POTS_CHANCE) {
                PotOfGold pot = _potManager.spawnPotOfGold(leprechaun.getLocation());
                if (pot != null) {
                    getLogger().info(
                        "Spawned pot of gold at " + Util.formatLocation(pot.getLocation()) +
                        " alive for " + pot.getLifeInTicks() + " ticks.");
                    ItemStack map = new ItemStack(Material.PAPER, 1);
                    ItemMeta meta = map.getItemMeta();
                    meta.setDisplayName(ChatColor.GOLD + "Treasure Map");
                    String formattedLoc = pot.getLocation().getBlockX() + ", " +
                                          pot.getLocation().getBlockY() + ", " +
                                          pot.getLocation().getBlockZ();
                    meta.setLore(Lists.newArrayList(
                        "Aye, ye got me!",
                        "Me treasure is at " + formattedLoc + ".",
                        "But ye better shake a leg!",
                        "Nothing lasts forever."));
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
     * The drop chance increases by 20% of the base level per looting level.
     *
     * @param lootingLevel the looting level of the weapon.
     * @return a factor to be multiplied by the base drop chance to compute the
     *         actual drop chance.
     */
    protected double adjustedChance(int lootingLevel) {
        return 1.0 + 0.2 * lootingLevel;
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
     * Metadata name used for metadata stored on mobs to record looting
     * enchantment level of Looting weapon used by a player.
     */
    protected static final String PLAYER_LOOTING_LEVEL_KEY = "Leprechauns_PlayerLootingLevel";

    /**
     * Time in ticks (1/20ths of a second) for which player attack damage
     * "sticks" to a mob. The time between the last player damage on a mob and
     * its death must be less than this for it to drop special stuff.
     */
    protected static final int PLAYER_DAMAGE_TICKS = 100;

    /**
     * Manages pots of gold.
     */
    protected PotManager _potManager = new PotManager();
} // class Leprechauns
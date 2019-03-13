package io.totemo.leprechauns;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

// ----------------------------------------------------------------------------
/**
 * Represents the state of one pot of gold.
 *
 * Pots of gold are depicted using a retextured cauldron.
 */
public class PotOfGold {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param loc the location of the pot.
     * @param lifeTicks the number of ticks this pot of gold should live.
     */
    public PotOfGold(Location loc, int lifeTicks) {
        _location = loc.clone();
        _lifeTicks = lifeTicks;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the cauldron location.
     *
     * @return the cauldron location.
     */
    public Location getLocation() {
        return _location;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the cauldron block.
     *
     * @return the cauldron block.
     */
    public Block getBlock() {
        return _location.getBlock();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the remaining life time in ticks.
     *
     * @return the remaining life time in ticks.
     */
    public int getLifeInTicks() {
        return _lifeTicks;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the pot is still alive (has not been found or timed out).
     *
     * This method is intended to be called once per tick for each pot of gold
     * in existence. It also updates particle effects around the pot.
     *
     * @return true if the pot is still alive (has not been found or timed out).
     */
    public boolean isAlive() {
        --_lifeTicks;
        if (_lifeTicks <= 0) {
            Leprechauns.PLUGIN.getLogger().info("Pot at " + Util.formatLocation(_location) + " timed out.");
            return false;
        }

        Location particleCentre = _location.clone();
        particleCentre.add(0.5, 0.5, 0.5);
        _location.getWorld().spawnParticle(Particle.REDSTONE,
                                           particleCentre,
                                           Leprechauns.CONFIG.POTS_PARTICLE_COUNT,
                                           Leprechauns.CONFIG.POTS_PARTICLE_RADIUS,
                                           Leprechauns.CONFIG.POTS_PARTICLE_RADIUS,
                                           Leprechauns.CONFIG.POTS_PARTICLE_RADIUS,
                                           POT_DUST_OPTIONS);
        for (Entity entity : _location.getWorld().getNearbyEntities(_location, 5, 5, 5)) {
            if (entity instanceof Player) {
                spawnLoot((Player) entity);
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Spawn loot at the pot location.
     *
     * @param player the player who found the pot.
     */
    public void spawnLoot(Player player) {
        Leprechauns.PLUGIN.getLogger().info("Pot broken by " + player.getName() + " " + Util.formatLocation(getLocation()));
        spawnFirework();

        World world = _location.getWorld();
        world.playSound(getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 3, 1);
        for (Drop drop : Leprechauns.CONFIG.DROPS_POTS) {
            if (drop.isValid() && Math.random() < drop.getDropChance()) {
                world.dropItemNaturally(_location, drop.generate());
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Spawn a firework the at pot location.
     */
    protected void spawnFirework() {
        World world = _location.getWorld();
        Firework firework = (Firework) world.spawnEntity(_location, EntityType.FIREWORK);
        if (firework != null) {
            FireworkEffect.Builder builder = FireworkEffect.builder();
            if (Math.random() < 0.3) {
                builder.withFlicker();
            }
            if (Math.random() < 0.3) {
                builder.withTrail();
            }

            builder.with(FIREWORK_TYPES[Util.randomInt(FIREWORK_TYPES.length)]);
            final int primaryColors = 1 + Util.randomInt(4);
            for (int i = 0; i < primaryColors; ++i) {
                int darker = Util.randomInt(128);
                builder.withColor(Color.fromRGB(255 - darker / 2, 255 - darker, Util.randomInt(64)));
            }

            final int fadeColors = 1 + Util.randomInt(3);
            builder.withFade(Color.fromRGB(255, 255, 255));
            for (int i = 0; i < fadeColors; ++i) {
                builder.withColor(Color.fromRGB(255, 255, 128 + Util.randomInt(64)));
            }

            FireworkMeta meta = firework.getFireworkMeta();
            meta.setPower(Util.randomInt(2));
            meta.addEffect(builder.build());
            firework.setFireworkMeta(meta);
        }
    } // spawnFirework

    // ------------------------------------------------------------------------
    /**
     * Remove the pot of gold (cauldron) by turning it into air.
     */
    public void vaporise() {
        getBlock().setType(Material.AIR);
    }

    // ------------------------------------------------------------------------
    /**
     * Firework types.
     */
    protected static final FireworkEffect.Type[] FIREWORK_TYPES = { Type.BALL, Type.BALL_LARGE, Type.STAR, Type.BURST };

    /**
     * Restone dust particle options for pots of gold.
     */
    protected static final DustOptions POT_DUST_OPTIONS = new DustOptions(Color.fromRGB(0xffd700), 1.0f);

    /**
     * Location of the pot of gold.
     */
    protected Location _location;

    /**
     * The number of ticks this pot of gold should live.
     */
    protected int _lifeTicks;

} // class PotOfGold

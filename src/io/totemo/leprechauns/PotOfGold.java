package io.totemo.leprechauns;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import com.darkblade12.particleeffect.ParticleEffect;

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

        ParticleEffect.REDSTONE.display(2.0f, 2.0f, 2.0f, 1.0f, 100, _location, 32);
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
        world.playSound(_location, Sound.LEVEL_UP, 20, 1);
        world.dropItemNaturally(_location, new ItemStack(Material.GOLD_INGOT, Util.random(1, 10)));
        world.dropItemNaturally(_location, new ItemStack(Material.GOLD_NUGGET, Util.random(1, 10)));
        world.dropItemNaturally(_location, new ItemStack(Material.GOLDEN_CARROT, Util.random(1, 5)));
        if (Math.random() < 0.25) {
            world.dropItemNaturally(_location, new ItemStack(Material.GOLDEN_APPLE, 1, (short) 1));
        }
        if (Math.random() < 0.25) {
            world.dropItemNaturally(_location, new ItemStack(Material.GOLD_BARDING, 1));
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

            builder.with(FIREWORK_TYPES[_random.nextInt(FIREWORK_TYPES.length)]);
            final int primaryColors = 1 + _random.nextInt(4);
            for (int i = 0; i < primaryColors; ++i) {
                int darker = _random.nextInt(128);
                builder.withColor(Color.fromRGB(255 - darker / 2, 255 - darker, _random.nextInt(64)));
            }

            final int fadeColors = 1 + _random.nextInt(3);
            builder.withFade(Color.fromRGB(255, 255, 255));
            for (int i = 0; i < fadeColors; ++i) {
                builder.withColor(Color.fromRGB(255, 255, 128 + _random.nextInt(64)));
            }

            FireworkMeta meta = firework.getFireworkMeta();
            meta.setPower(_random.nextInt(2));
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
     * Random number generator.
     */
    protected Random _random = new Random();

    /**
     * Location of the pot of gold.
     */
    protected Location _location;

    /**
     * The number of ticks this pot of gold should live.
     */
    protected int _lifeTicks;
} // class PotOfGold
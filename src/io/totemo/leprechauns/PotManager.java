package io.totemo.leprechauns;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

// ----------------------------------------------------------------------------
/**
 * Manages the collection of all pots of gold in the world.
 */
public class PotManager {
    // ------------------------------------------------------------------------
    /**
     * If the maximum number of extant pots of gold has not been exceeded, then
     * Calculate a random location within the configured range limits of the
     * death point, and spawn a pot of gold there.
     *
     * This method should only be called if the death location was in the
     * affected world.
     *
     * The pot will not be spawned if it might collide with an entity (e.g. a
     * painting or item frame) - thus breaking it, if it would be above the
     * height limit of the world, if the maximum number of pots currently exists
     * in the world, or if the pot would be over the world border.
     *
     * @param deathLocation death location in the affected world.
     * @return a new PotOfGold, or null if it could not be spawned.
     */
    public PotOfGold spawnPotOfGold(Location deathLocation) {
        if (_pots.size() >= Leprechauns.CONFIG.POTS_MAX) {
            return null;
        }

        double angleRadians = 2 * Math.PI * Math.random();
        double range = Util.random(Leprechauns.CONFIG.POTS_RANGE_MIN, Leprechauns.CONFIG.POTS_RANGE_MAX);
        double dX = range * Math.cos(angleRadians);
        double dZ = range * Math.sin(angleRadians);

        World world = deathLocation.getWorld();
        Block floorBlock = world.getHighestBlockAt(
            (int) (deathLocation.getX() + dX),
            (int) (deathLocation.getZ() + dZ));
        Location potLocation = floorBlock.getLocation();

        if (Math.abs(potLocation.getBlockX()) < Leprechauns.CONFIG.WORLD_BORDER &&
            Math.abs(potLocation.getBlockZ()) < Leprechauns.CONFIG.WORLD_BORDER &&
            potLocation.getBlockY() < world.getMaxHeight() - 1) {
            // Don't spawn the cauldron if it might break item frames/paintings.
            Collection<Entity> entities = potLocation.getWorld().getNearbyEntities(potLocation, 2, 2, 2);
            if (entities.size() == 0) {
                double distance = potLocation.distance(deathLocation);
                Leprechauns.PLUGIN.getLogger().info("Distance " + distance);
                Leprechauns.PLUGIN.getLogger().info("Travel ticks " +
                                                    (20 * (int) (distance / Leprechauns.CONFIG.POTS_MIN_PLAYER_SPEED)));
                int lifeInTicks = Leprechauns.CONFIG.POTS_EXTRA_TICKS +
                                  20 * (int) (distance / Leprechauns.CONFIG.POTS_MIN_PLAYER_SPEED);
                Leprechauns.PLUGIN.getLogger().info("Life in ticks " + lifeInTicks);

                // This check should be redundant after the height check.
                if (potLocation.getBlock().getType() == Material.AIR) {
                    PotOfGold pot = new PotOfGold(potLocation, lifeInTicks);
                    _pots.put(pot.getBlock(), pot);
                    pot.getBlock().setType(Material.CAULDRON);
                    return pot;
                }
            }
        }
        return null;
    } // spawnPotOfGold

    // ------------------------------------------------------------------------
    /**
     * Tick all pots in the world, updating particle effects and removing those
     * that were found or timed out.
     */
    public void tickAll() {
        Iterator<Entry<Block, PotOfGold>> it = _pots.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Block, PotOfGold> entry = it.next();
            if (!entry.getValue().isAlive()) {
                entry.getValue().vaporise();
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If the block is a pot of gold, return the corresponding {@link PotOfGold}
     * instance.
     *
     * @param block the block that might be a cauldron.
     * @return the {@link PotOfGold} or null if there is none at the specified
     *         Block.
     */
    public PotOfGold getPotOfGold(Block block) {
        if (block.getType() == Material.CAULDRON) {
            PotOfGold pot = _pots.get(block);
            if (pot != null) {
                return pot;
            }

            // TODO Remove this code once you are certain that the above works.
            for (Entry<Block, PotOfGold> entry : _pots.entrySet()) {
                if (entry.getKey().equals(block)) {
                    Leprechauns.PLUGIN.getLogger().warning("Needed to do a linear search for pot.");
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link PotOfGold} from the the world, thus cancelling the
     * particle effects.
     *
     * @param pot the PotOfGold.
     */
    public void removePot(PotOfGold pot) {
        pot.vaporise();
        _pots.remove(pot.getBlock());
    }

    // ------------------------------------------------------------------------
    /**
     * Remove all the pots.
     */
    public void removeAll() {
        Iterator<Entry<Block, PotOfGold>> it = _pots.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Block, PotOfGold> entry = it.next();
            entry.getValue().vaporise();
            it.remove();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Map from Location to PotOfGold at that location.
     */
    protected HashMap<Block, PotOfGold> _pots = new HashMap<Block, PotOfGold>();

} // class PotManager
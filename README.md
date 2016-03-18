Leprechauns
===========
Diminutive, drunken, surly Irish people.


Features
--------
Leprechauns is a St Patrick's Day themed Bukkit plugin that implements the
following features:

 * The plugin affects hostile mobs in a single configured world (by default,
   the overworld) only. Spawner mobs are not modified.
 * Affected hostile mobs are replaced with zombies dressed in leather armour
   (or skinned with a server resource pack), referred to as leprechauns.
 * For custom mobs to drop special drops when they die, they must have been
   recently hurt by a player.
 * The drop chance is improved by a looting sword.
 * Leprechauns are armed with shillelaghs (enchanted sticks) and shamrocks
   (enchanted long grass), with a small chance of the held item dropping.
 * Leprechauns also drop various potions named as alcoholic beverages, as well
   as gold nuggets.
 * Leprechauns have custom Irish names.
 * A small percentage of leprechauns drop a piece of paper named a "Treasure
   Map", which has as its lore the coordinates of a pot of gold (POG).
 * A POG is a cauldron that is spawned into the affected world (only) for a
   limited period of time within a range of distances from the leprechaun's
   death point.
 * The POG is marked with a rainbow of coloured particles. If the player breaks
   it or moves to within 5 blocks of it, then it disappears and drops
   configurable loot (typically golden carrots, ingots and nuggets).


Planned Improvements
--------------------

Due to development time constraints, the weapons and drops are hard-wired into
the code rather than being defined in the configuration file.  Making these
configurable, as originally planned, would improve the plugin.


Commands
--------

 * `/leprechauns reload` - Reload the plugin configuration.


Permissions
-----------

 * `leprechauns.admin` - Permission to administer the plugin (run `/leprechauns reload`).


Minecraft 1.9 Compatibility Issues
----------------------------------
Preliminary testing on a 1.9 Spigot server revealed that:

 * The XP level-up sound enum has changed its name in 1.9 and would need to be
   renamed in the code for 1.9 compatibility.
 * The Zombie.setVillager(false) statement in the code does not appear to be
   doing anything at all, and would need to change to
   Zombie.setVillagerProfession(null).
 * There may be other minor changes needed to make the plugin 1.9-compatible.

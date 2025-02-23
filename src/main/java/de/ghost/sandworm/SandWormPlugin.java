package de.ghost.sandworm;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SandWormPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Sand Worm Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Sand Worm Plugin Disabled!");
    }

    // Custom spawn method
    public void spawnSandWorm(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Using Zombie as a base since Minecraft doesn't have a Worm entity
        Mob sandWorm = (Mob) world.spawnEntity(location, EntityType.ZOMBIE);
        sandWorm.setCustomName("Sand Worm");
        sandWorm.setCustomNameVisible(true);
        sandWorm.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(200.0);
        sandWorm.setHealth(200.0);
        sandWorm.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(15.0);
        sandWorm.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25);

        // Add the tag "sandworm" for identification
        sandWorm.addScoreboardTag("sandworm");

        // Behavior: Burrow underground and ambush
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!sandWorm.isValid()) {
                    this.cancel();
                    return;
                }
                Location loc = sandWorm.getLocation();
                loc.setY(loc.getY() - 1); // Move underground slightly
                sandWorm.teleport(loc);

                // Ambush after a delay
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location above = sandWorm.getLocation();
                        above.setY(above.getY() + 2); // Jump up to ambush
                        sandWorm.teleport(above);
                    }
                }.runTaskLater(SandWormPlugin.this, 100); // 5 seconds later
            }
        }.runTaskTimer(this, 0, 200); // Every 10 seconds
    }

    // Event to spawn the Sand Worm in deserts
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getLocation().getBlock().getBiome().name().contains("DESERT")) {
            if (Math.random() < 0.05) { // 5% chance to spawn
                spawnSandWorm(event.getLocation());
            }
        }
    }

    // Command Handling
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spawnsandworm")) {
            if (sender instanceof Player player) {
                Location loc = player.getLocation();
                spawnSandWorm(loc);
                player.sendMessage("§aSand Worm spawned at your location!");
            } else if (sender instanceof ConsoleCommandSender) {
                getLogger().info("This command can only be used by a player.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("killsandworm")) {
            int count = 0;
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getScoreboardTags().contains("sandworm")) {
                        entity.remove();
                        count++;
                    }
                }
            }
            sender.sendMessage("§c" + count + " Sand Worm(s) killed.");
            return true;
        }

        return false;
    }
}

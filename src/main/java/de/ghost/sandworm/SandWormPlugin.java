package de.ghost.sandworm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.BitField;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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


    public void spawnSandWorm(Location location) {
        World world = location.getWorld();
        if (world == null) return;


/*
        Location location01 = new Location(Bukkit.getWorld("world"), 100, 65, 200);
        World world01 = location01.getWorld();
        world01.spawnEntity(location, EntityType.ZOMBIE);
*/


        // Coordinaten des Spielers
        Location initialLocation;
        initialLocation = location.clone().add(0, 0, 0);

        // Verwende einen Zombie als Basis
        Mob sandWorm = (Mob) world.spawnEntity(initialLocation, EntityType.ZOMBIE);
        sandWorm.setCustomName("Sand Worm");
        sandWorm.setCustomNameVisible(true);
        sandWorm.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200.0);
        sandWorm.setHealth(200.0);
        sandWorm.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(15.0);
        sandWorm.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.25);

        // Unterdrücke alle Geräusche
        sandWorm.setSilent(true);


        // Füge den Tag "sandworm" zur Identifikation hinzu
        sandWorm.addScoreboardTag("sandworm");

        // Stelle sicher, dass nur große Zombies gespawnt werden und das Monster nicht despawnt:
        if (sandWorm instanceof Zombie) {
            Zombie zombie = (Zombie) sandWorm;
            zombie.setBaby(false);                     // Nur große Zombies
            zombie.setRemoveWhenFarAway(false);          // Nicht despawnen, wenn der Chunk entladen wird
        }

        // Hole Coordinaten vom Sandwurm - Coordinate Y
        double normalHeight = sandWorm.getLocation().getY();
        Bukkit.broadcastMessage("Sand Worm spawn Y-Koordinate: " + normalHeight);
        double underGroundHeight = normalHeight - 10;

        // Erstelle eine gelbe Bossbar für diesen Sand Worm
        BossBar bossBar = Bukkit.createBossBar("Sand Worm Health", BarColor.YELLOW, BarStyle.SOLID);
        bossBar.setProgress(sandWorm.getHealth() / sandWorm.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        // Aktualisiere die Bossbar jede Sekunde
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!sandWorm.isValid() || sandWorm.getHealth() <= 0 || !sandWorm.getScoreboardTags().contains("sandworm")) {
                    bossBar.removeAll();
                    this.cancel();
                    return;
                }
                double progress = sandWorm.getHealth() / sandWorm.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                bossBar.setProgress(progress);
            }
        }.runTaskTimer(this, 0, 20);

        // Erstelle eine Liste von 13 Zielpositionen relativ zur Spawnposition (base)
        Location base = location.clone();
        List<Location> targetPositions = new ArrayList<>();
        targetPositions.add(new Location(world, base.getX() - 75, base.getY(), base.getZ() - 75)); // 0
        targetPositions.add(new Location(world, base.getX(), base.getY(), base.getZ() - 75)); // 1
        targetPositions.add(new Location(world, base.getX() + 75, base.getY(), base.getZ() - 75)); // 2
        targetPositions.add(new Location(world, base.getX() - 35, base.getY(), base.getZ() - 35)); // 3
        targetPositions.add(new Location(world, base.getX() + 35, base.getY(), base.getZ() - 35)); // 4
        targetPositions.add(new Location(world, base.getX() - 75, base.getY(), base.getZ()));      // 5
        targetPositions.add(new Location(world, base.getX(), base.getY(), base.getZ()));         // 6 (Spawnposition)
        targetPositions.add(new Location(world, base.getX() + 75, base.getY(), base.getZ()));      // 7
        targetPositions.add(new Location(world, base.getX() - 35, base.getY(), base.getZ() + 35)); // 8
        targetPositions.add(new Location(world, base.getX() + 35, base.getY(), base.getZ() + 35)); // 9
        targetPositions.add(new Location(world, base.getX() - 75, base.getY(), base.getZ() + 75)); // 10
        targetPositions.add(new Location(world, base.getX(), base.getY(), base.getZ() + 75)); // 11
        targetPositions.add(new Location(world, base.getX() + 75, base.getY(), base.getZ() + 75)); // 12

        // Setze initial das Ziel auf die Spawnposition (Index 6)
        final Location[] currentTarget = new Location[]{ targetPositions.get(6) };

        // Bewege den Zombie kontinuierlich in Richtung des aktuellen Ziels ("hingleiten")
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!sandWorm.isValid()) {
                    this.cancel();
                    return;
                }

                // Change Code from here

                Location currentLoc = sandWorm.getLocation();
                Location targetLoc = currentTarget[0].clone();  // Clone des Zielortes

                // Hier den Y-Wert angleichen, damit nur X und Z berücksichtigt werden:
                targetLoc.setY(currentLoc.getY());


                double distance = currentLoc.distance(targetLoc);
                if (distance > 0.5) {
                    Vector direction = targetLoc.toVector().subtract(currentLoc.toVector());
                    direction.normalize();
                    // Verwende die normale Laufgeschwindigkeit (vom Attribut)
                    double speed = sandWorm.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue();
                    direction.multiply(speed);
                    sandWorm.setVelocity(direction);
                } else {
                    // Ziel erreicht
                    sandWorm.setVelocity(new Vector(0, 0, 0));
                    Bukkit.broadcastMessage("[DEBUG] Sand Worm: Ziel erreicht: " + targetLoc.toVector().toString());

                    // Generiere eine neue zufällige Y-Koordinate (entweder ~-20 oder ~)
                    int randomLocationType = ThreadLocalRandom.current().nextInt(0, 100);
                    if (randomLocationType < 20) { // 20% Chance unter dem Boden (~-20)
                        Location newLocation = currentLoc.clone();
                        newLocation.setY(underGroundHeight); // unterGroundHeight sollte z. B. -20 sein
                        sandWorm.teleport(newLocation);
                        Bukkit.broadcastMessage("[DEBUG] Sand Worm teleportiert sich unter den Boden: Y = " + underGroundHeight);
                    } else { // 80% Chance auf normale Höhe (~)
                        Location newLocation = currentLoc.clone();
                        newLocation.setY(normalHeight); // normalHeight könnte z. B. 236 sein
                        sandWorm.teleport(newLocation);
                        Bukkit.broadcastMessage("[DEBUG] Sand Worm bleibt auf normaler Höhe: Y = " + normalHeight);
                    }

                    // Wähle ein neues Ziel aus der Liste
                    int randomIndex = ThreadLocalRandom.current().nextInt(0, targetPositions.size());
                    currentTarget[0] = targetPositions.get(randomIndex);
                    Bukkit.broadcastMessage("[DEBUG] Sand Worm: Neuer Zielpunkt ausgewählt: Index "
                            + randomIndex + " -> " + currentTarget[0].toVector().toString());



                    // To here
                }
            }
        }.runTaskTimer(this, 0, 1);
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
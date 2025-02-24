package de.ghost.sandworm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SandWormPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {}, this);
        getLogger().info("Sand Worm Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Sand Worm Plugin Disabled!");
    }

    public void spawnSandWorm(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Location initialLocation = location.clone();
        ArmorStand head = (ArmorStand) world.spawnEntity(initialLocation, EntityType.ARMOR_STAND);
        head.setCustomName("Sand Worm");
        head.setCustomNameVisible(true);
        head.setGravity(false);
        if (head.getAttribute(Attribute.MAX_HEALTH) != null) {
            head.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200.0);
            head.setHealth(200.0);
        }
        head.setSilent(true);
        head.addScoreboardTag("sandworm");

        double normalHeight = head.getLocation().getY();
        Bukkit.broadcastMessage("Sand Worm spawn Y-Koordinate: " + normalHeight);
        double underGroundHeight = normalHeight - 10;

        BossBar bossBar = Bukkit.createBossBar("Sand Worm Health", BarColor.YELLOW, BarStyle.SOLID);
        bossBar.setProgress(head.getHealth() / head.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }


        List<ArmorStand> snakeParts = new ArrayList<>();
        snakeParts.add(head);
        LinkedList<Location> headTrail = new LinkedList<>();
        headTrail.add(head.getLocation().clone());

        final int delayFactor = 4;
        final int maxLength = 30;

        Location base = location.clone();
        List<Location> targetPositions = new ArrayList<>();
        targetPositions.add(new Location(world, base.getX() - 75, base.getY(), base.getZ() - 75)); // 0
        targetPositions.add(new Location(world, base.getX(), base.getY(), base.getZ() - 75));      // 1
        targetPositions.add(new Location(world, base.getX() + 75, base.getY(), base.getZ() - 75)); // 2
        targetPositions.add(new Location(world, base.getX() - 35, base.getY(), base.getZ() - 35)); // 3
        targetPositions.add(new Location(world, base.getX() + 35, base.getY(), base.getZ() - 35)); // 4
        targetPositions.add(new Location(world, base.getX() - 75, base.getY(), base.getZ()));      // 5
        targetPositions.add(new Location(world, base.getX(), base.getY(), base.getZ()));         // 6 (Spawnposition)
        targetPositions.add(new Location(world, base.getX() + 75, base.getY(), base.getZ()));      // 7
        targetPositions.add(new Location(world, base.getX() - 35, base.getY(), base.getZ() + 35)); // 8
        targetPositions.add(new Location(world, base.getX() + 35, base.getY(), base.getZ() + 35)); // 9
        targetPositions.add(new Location(world, base.getX() - 75, base.getY(), base.getZ() + 75)); // 10
        targetPositions.add(new Location(world, base.getX(), base.getY(), base.getZ() + 75));      // 11
        targetPositions.add(new Location(world, base.getX() + 75, base.getY(), base.getZ() + 75)); // 12

        final Location[] currentTarget = new Location[]{ targetPositions.get(6) };

        new BukkitRunnable() {
            Vector currentDirection = currentTarget[0].toVector().subtract(initialLocation.toVector()).normalize();
            int tickCount = 0;
            @Override
            public void run() {
                if (!snakeParts.get(0).isValid() || snakeParts.get(0).getHealth() <= 0 ||
                        !snakeParts.get(0).getScoreboardTags().contains("sandworm")) {
                    bossBar.removeAll();
                    this.cancel();
                    return;
                }

                ArmorStand head = snakeParts.get(0);
                Location currentLoc = head.getLocation();
                Location targetLoc = currentTarget[0].clone();
                targetLoc.setY(currentLoc.getY());
                double distance = currentLoc.distance(targetLoc);
                double speed = 0.25;

                if (distance > 0.5) {
                    Vector desiredDirection = targetLoc.toVector().subtract(currentLoc.toVector()).normalize();
                    double maxTurnAngle = Math.toRadians(2);
                    double angleDiff = currentDirection.angle(desiredDirection);
                    if (angleDiff > maxTurnAngle) {
                        Vector axis = currentDirection.clone().crossProduct(desiredDirection);
                        if (axis.length() != 0) {
                            axis.normalize();
                            currentDirection = rotateVector(currentDirection, axis, maxTurnAngle);
                        } else {
                            currentDirection = desiredDirection;
                        }
                    } else {
                        currentDirection = desiredDirection;
                    }
                    Vector move = currentDirection.clone().multiply(speed);
                    Location newLoc = currentLoc.clone().add(move);
                    head.teleport(newLoc);
                } else {
                    head.teleport(currentLoc);
                    Bukkit.broadcastMessage("[DEBUG] Sand Worm: Ziel erreicht: " + targetLoc.toVector().toString());
                    int randomLocationType = ThreadLocalRandom.current().nextInt(0, 100);
                    if (randomLocationType < 20) {
                        Location newLocation = currentLoc.clone();
                        newLocation.setY(underGroundHeight);
                        head.teleport(newLocation);
                        Bukkit.broadcastMessage("[DEBUG] Sand Worm teleportiert sich unter den Boden: Y = " + underGroundHeight);
                    } else {
                        Location newLocation = currentLoc.clone();
                        newLocation.setY(normalHeight);
                        head.teleport(newLocation);
                        Bukkit.broadcastMessage("[DEBUG] Sand Worm bleibt auf normaler Höhe: Y = " + normalHeight);
                    }
                    int randomIndex = ThreadLocalRandom.current().nextInt(0, targetPositions.size());
                    currentTarget[0] = targetPositions.get(randomIndex);
                    Bukkit.broadcastMessage("[DEBUG] Sand Worm: Neuer Zielpunkt ausgewählt: Index "
                            + randomIndex + " -> " + currentTarget[0].toVector().toString());
                }

                double progress = head.getHealth() / head.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                bossBar.setProgress(progress);

                tickCount++;
                headTrail.add(head.getLocation().clone());
                int maxTrailSize = maxLength * delayFactor + 1;
                if (headTrail.size() > maxTrailSize) {
                    headTrail.removeFirst();
                }

                for (int i = 1; i < snakeParts.size(); i++) {
                    int index = headTrail.size() - 1 - (i * delayFactor);
                    if (index >= 0) {
                        Location targetPos = headTrail.get(index);
                        snakeParts.get(i).teleport(targetPos);
                    }
                }

                if (snakeParts.size() < maxLength) {
                    int requiredTrailLength = snakeParts.size() * delayFactor + 1;
                    if (headTrail.size() >= requiredTrailLength) {
                        int newSegmentIndex = headTrail.size() - 1 - (snakeParts.size() * delayFactor);
                        if (newSegmentIndex >= 0) {
                            Location spawnLoc = headTrail.get(newSegmentIndex);
                            ArmorStand newSegment = (ArmorStand) head.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
                            newSegment.setCustomName("Sand Worm");
                            newSegment.setCustomNameVisible(true);
                            newSegment.setGravity(false);
                            if (newSegment.getAttribute(Attribute.MAX_HEALTH) != null) {
                                newSegment.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200.0);
                                newSegment.setHealth(200.0);
                            }
                            newSegment.setSilent(true);
                            newSegment.addScoreboardTag("sandworm");
                            snakeParts.add(newSegment);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 1);
    }

    private Vector rotateVector(Vector vector, Vector axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return vector.clone().multiply(cos)
                .add(axis.clone().crossProduct(vector).multiply(sin))
                .add(axis.clone().multiply(axis.dot(vector) * (1 - cos)));
    }

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

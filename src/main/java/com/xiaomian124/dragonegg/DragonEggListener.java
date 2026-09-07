package com.xiaomian124.dragonegg;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DragonEggListener implements Listener {

    private final DragonEgg plugin;
    private static final ConcurrentHashMap<Location, BukkitTask> EGG_TASKS = new ConcurrentHashMap<>();
    private static final int[][] CRYSTAL_POSITIONS = {{-3, 0}, {3, 0}, {0, -3}, {0, 3}};
    private static final ConcurrentHashMap<UUID, Location> PLAYER_EGG = new ConcurrentHashMap<>();

    public DragonEggListener(DragonEgg plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDragonEggPlace(BlockPlaceEvent event) {
        Block eggBlock = event.getBlock();
        if (eggBlock.getType() != Material.DRAGON_EGG) {
            return;
        }

        Location eggLocation = eggBlock.getLocation();
        World world = eggLocation.getWorld();

        if (world.getEnvironment() != World.Environment.THE_END) {
            return;
        }

        Block below = eggBlock.getRelative(BlockFace.DOWN);
        Block belowBelow = below.getRelative(BlockFace.DOWN);
        if (below.getType() != Material.BEDROCK || belowBelow.getType() != Material.BEDROCK) {
            return;
        }
        if (Math.abs(eggLocation.getBlockX()) > 1 || Math.abs(eggLocation.getBlockZ()) > 1) {
            return;
        }

        if (EGG_TASKS.containsKey(eggLocation)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(gradientText("[BDWL] 这个位置已经有龙蛋在等待被孵化了！"));
            return;
        }

        boolean dragonExists = world.getEntitiesByClass(EnderDragon.class).stream()
                .anyMatch(dragon -> !dragon.isDead());
        if (dragonExists) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(gradientText("[BDWL] 末影龙仍在战斗中，请击败再尝试孵化新的！"));
            return;
        }

        event.getPlayer().sendMessage(gradientText("[BDWL] 龙蛋已放置于祭坛之上，孵化时间大约 3分钟 ！"));

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (eggBlock.getType() == Material.DRAGON_EGG) {
                    spawnEnderDragon(world, eggLocation);
                }
                EGG_TASKS.remove(eggLocation);
                PLAYER_EGG.entrySet().removeIf(entry -> entry.getValue().equals(eggLocation));
            }
        }.runTaskLater(plugin, 3600L);

        EGG_TASKS.put(eggLocation, task);

        UUID playerId = event.getPlayer().getUniqueId();
        PLAYER_EGG.put(playerId, eggLocation);
    }

    @EventHandler
    public void onDragonEggBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DRAGON_EGG) {
            return;
        }

        Location loc = block.getLocation();
        BukkitTask task = EGG_TASKS.remove(loc);
        if (task != null) {
            task.cancel();
            PLAYER_EGG.entrySet().removeIf(entry -> entry.getValue().equals(loc));
            event.getPlayer().sendMessage(gradientText("[BDWL] 龙蛋被破坏，孵化过程已取消！"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("RIGHT_CLICK_BLOCK")) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.DRAGON_EGG) {
            return;
        }

        Location loc = clickedBlock.getLocation();
        BukkitTask task = EGG_TASKS.remove(loc);
        if (task != null) {   // 添加 null 检查
            task.cancel();
            PLAYER_EGG.entrySet().removeIf(entry -> entry.getValue().equals(loc));
            event.getPlayer().sendMessage(gradientText("[BDWL] 龙蛋移动了，孵化过程已取消！"));
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World from = event.getFrom();
        if (from.getEnvironment() != World.Environment.THE_END) return;

        UUID uuid = player.getUniqueId();
        Location loc = PLAYER_EGG.get(uuid);
        if (loc != null && EGG_TASKS.containsKey(loc)) {
            event.getPlayer().sendMessage(gradientText("[BDWL] 您的龙蛋仍在末地祭坛孵化，召唤不会中断！"));
        }
    }

    private void spawnEnderDragon(World world, Location eggLocation) {
        world.getBlockAt(eggLocation).setType(Material.AIR);

        AtomicInteger placed = new AtomicInteger(0);

        for (int[] pos : CRYSTAL_POSITIONS) {
            int x = pos[0], z = pos[1];
            int bedrockY = -1;
            for (int y = 50; y <= 70; y++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.BEDROCK) {
                    bedrockY = y;
                } else if (bedrockY != -1) {
                    break;
                }
            }
            if (bedrockY == -1) {
                continue;
            }
            Location crystalLoc = new Location(world, x + 0.5, bedrockY + 1.5, z + 0.5);
            EnderCrystal crystal = (EnderCrystal) world.spawnEntity(crystalLoc, EntityType.END_CRYSTAL);
            placed.incrementAndGet();
        }

        if (placed.get() == 4) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    DragonBattle battle = world.getEnderDragonBattle();
                    if (battle != null) {
                        try {
                            battle.initiateRespawn();
                            world.getPlayers().forEach(p ->
                                    p.sendMessage(gradientText("[BDWL] ★ 末影龙即将涅磐重生 ★"))
                            );
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }.runTaskLater(plugin, 2L);
        } else {
            world.getPlayers().forEach(p ->
                    p.sendMessage(gradientText("[BDWL] 发生了一些问题：末影水晶生成不完整，基岩是否有破损？"))
            );
        }
    }

    private String gradientText(String text) {
        int[] startColor = {255, 0, 0};    // 红
        int[] midColor = {255, 255, 0};    // 黄
        int[] endColor = {255, 0, 255};    // 紫

        StringBuilder result = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            double progress = (double) i / length;

            int red = (int) (127 + 127 * Math.sin(progress * 2 * Math.PI + 0));
            int green = (int) (127 + 127 * Math.sin(progress * 2 * Math.PI + 2.094));
            int blue = (int) (127 + 127 * Math.sin(progress * 2 * Math.PI + 4.188));

            result.append("§x");
            result.append("§").append(hexDigit(red / 16));
            result.append("§").append(hexDigit(red % 16));
            result.append("§").append(hexDigit(green / 16));
            result.append("§").append(hexDigit(green % 16));
            result.append("§").append(hexDigit(blue / 16));
            result.append("§").append(hexDigit(blue % 16));
            result.append(text.charAt(i));
        }

        return result.toString();
    }

    private char hexDigit(int value) {
        value = Math.min(15, Math.max(0, value));
        return "0123456789abcdef".charAt(value);
    }

    public static void cancelAllTasks() {
        EGG_TASKS.values().forEach(BukkitTask::cancel);
        EGG_TASKS.clear();
        PLAYER_EGG.clear();
    }
}
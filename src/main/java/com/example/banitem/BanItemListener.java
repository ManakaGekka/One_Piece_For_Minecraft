package com.example.banitem;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Barrel;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.Map;
import java.util.Random;

public class BanItemListener implements Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();

    public BanItemListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerBan(PlayerKickEvent event) {
        if (!event.getReason().contains("banned")) return;

        Player player = event.getPlayer();
        // 生成两个相邻木桶位置（x轴偏移0,1）
        Location baseLoc = generateRandomLocation(player);
        Location barrelLoc1 = new Location(baseLoc.getWorld(), baseLoc.getX(), baseLoc.getY(), baseLoc.getZ());
        Location barrelLoc2 = new Location(baseLoc.getWorld(), baseLoc.getX() + 1, baseLoc.getY(), baseLoc.getZ());

        // 创建两个木桶
        barrelLoc1.getBlock().setType(Material.BARREL);
        barrelLoc2.getBlock().setType(Material.BARREL);

        Barrel barrel1 = (Barrel) barrelLoc1.getBlock().getState();
        Barrel barrel2 = (Barrel) barrelLoc2.getBlock().getState();
        // 检查Barrel实例是否有效
        if (barrel1 == null || barrel2 == null) {
            Bukkit.getLogger().severe("[BanItem] 木桶块状态获取失败，无法存储物品");
            return;
        }
        Inventory inv1 = barrel1.getInventory(); // 背包物品桶
        Inventory inv2 = barrel2.getInventory(); // 末影箱物品桶
        // 检查Inventory是否有效
        if (inv1 == null || inv2 == null) {
            Bukkit.getLogger().severe("[BanItem] 木桶Inventory获取失败，无法存储物品");
            return;
        }

                PlayerInventory playerInv = player.getInventory();

        // 存储背包所有物品到第一个木桶（包括副手物品）
        ItemStack offHandItem = playerInv.getItemInOffHand();
        if (offHandItem != null) {
            inv1.addItem(offHandItem);
        }
        for (ItemStack item : playerInv.getContents()) {
            if (item != null) {
                Map<Integer, ItemStack> remaining = inv1.addItem(item);
                if (!remaining.isEmpty()) {
                    Bukkit.getLogger().warning("[BanItem] 背包物品桶无法存储物品: " + item.getType() + " 数量: " + item.getAmount());
                }
            }
        }
        // 统计实际存储的非空气物品数量（含副手）
        long storedCount = inv1.all().values().stream()
                .filter(item -> item != null && item.getType() != Material.AIR)
                .count();
        Bukkit.getLogger().info("[BanItem] 实际存储背包物品数量（含副手）: " + storedCount);
        // 记录背包物品桶实际存储数量（非容量）
        Bukkit.getLogger().info("[BanItem] 背包物品桶已存储 " + storedCount + " 个物品");

        // 存储末影箱物品到第二个木桶
        Inventory enderChest = player.getEnderChest();
        for (ItemStack item : enderChest.getContents()) {
            if (item != null) {
                Map<Integer, ItemStack> remaining = inv2.addItem(item);
                if (!remaining.isEmpty()) {
                    Bukkit.getLogger().warning("[BanItem] 末影箱物品桶无法存储物品: " + item.getType() + " 数量: " + item.getAmount());
                }
            }
        }
        // 统计末影箱物品桶实际存储的非空气物品数量
        long enderStoredCount = inv2.all().values().stream()
                .filter(item -> item != null && item.getType() != Material.AIR)
                .count();
        Bukkit.getLogger().info("[BanItem] 末影箱物品桶已存储 " + enderStoredCount + " 个物品");
        enderChest.clear();

        // 保存木桶状态
        barrel1.update();
        barrel2.update();
        player.getInventory().clear();

        // 获取坐标加密开关配置
        boolean encryptCoords = plugin.getConfig().getBoolean("encrypt-coordinates", true);
        // 根据配置决定是否加密坐标
        String coords = encryptCoords ? encryptCoordinates(baseLoc) : getRawCoordinates(baseLoc);
        // 添加调试日志确认加密状态
        Bukkit.getLogger().info("[BanItem] 加密开关状态：" + encryptCoords + "，原始坐标：" + getRawCoordinates(baseLoc));
        String message = plugin.getConfig().getString("broadcast-message", "被封禁玩家 {player} 的物品已保存，位置：{coords}");
        message = message.replace("{player}", player.getName()).replace("{coords}", coords);
        Bukkit.broadcastMessage(message);
    }

    private Location generateRandomLocation(Player player) {
        Location playerLoc = player.getLocation();
        int x = playerLoc.getBlockX() + random.nextInt(50 * 2 + 1) - 50;
        int z = playerLoc.getBlockZ() + random.nextInt(50 * 2 + 1) - 50;
        int y = player.getWorld().getHighestBlockYAt(x, z) + 1;

        return new Location(player.getWorld(), x, y, z);
    }

    private String encryptCoordinates(Location loc) {
        int offset = 3;
        String x = String.valueOf(loc.getBlockX());
        String y = String.valueOf(loc.getBlockY());
        String z = String.valueOf(loc.getBlockZ());

        StringBuilder encrypted = new StringBuilder();
        for (char c : (x + "," + y + "," + z).toCharArray()) {
            encrypted.append((char) (c + offset));
        }
        return encrypted.toString();
    }

    private String getRawCoordinates(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
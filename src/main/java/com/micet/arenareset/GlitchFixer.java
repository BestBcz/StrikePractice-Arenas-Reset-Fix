package com.micet.arenareset;

import ga.strikepractice.events.DuelStartEvent;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityTeleport;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class GlitchFixer implements Listener {

    private final JavaPlugin plugin;

    // 设置触发修复的距离阈值 (方块)
    private static final double FIX_DISTANCE = 50.0;
    private static final double FIX_DISTANCE_SQ = FIX_DISTANCE * FIX_DISTANCE;

    public GlitchFixer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDuelStart(DuelStartEvent event) {
        // --- 核心修复 START ---
        // 1. 获取玩家名字 (API 返回的是 String)
        String p1Name = event.getFight().getP1();
        String p2Name = event.getFight().getP2();

        // 2. 通过名字获取在线玩家对象
        final Player p1 = Bukkit.getPlayer(p1Name);
        final Player p2 = Bukkit.getPlayer(p2Name);

        // 如果玩家不在线，直接返回
        if (p1 == null || p2 == null) return;

        // 启动“雷达”任务
        new BukkitRunnable() {
            int timeout = 120; // 2分钟超时

            @Override
            public void run() {
                if (!p1.isOnline() || !p2.isOnline() || p1.isDead() || p2.isDead()) {
                    this.cancel();
                    return;
                }

                if (p1.getWorld() != p2.getWorld()) {
                    return;
                }

                double distSq = p1.getLocation().distanceSquared(p2.getLocation());

                if (distSq < FIX_DISTANCE_SQ) {
                    refreshPlayerPosition(p1);
                    refreshPlayerPosition(p2);
                    this.cancel();
                }

                timeout--;
                if (timeout <= 0) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void refreshPlayerPosition(Player target) {
        if (target == null || !target.isOnline()) return;

        try {
            EntityPlayer nmsPlayer = ((CraftPlayer) target).getHandle();
            PacketPlayOutEntityTeleport packet = new PacketPlayOutEntityTeleport(nmsPlayer);

            for (Player observer : target.getWorld().getPlayers()) {
                if (observer != target && observer.canSee(target)) {
                    ((CraftPlayer) observer).getHandle().playerConnection.sendPacket(packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
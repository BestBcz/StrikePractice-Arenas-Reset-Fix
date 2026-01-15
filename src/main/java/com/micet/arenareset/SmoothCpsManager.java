package com.micet.arenareset;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SmoothCpsManager implements Listener {

    private final JavaPlugin plugin;

    // --- 限制设置 ---
    private static final int MAX_CPS = 12; // 限制为 12 CPS
    private static final long MIN_DELAY_MS = 1000 / MAX_CPS;

    // --- Combo 判定阈值 (参考 MisplaceManager) ---
    private static final long ATTACK_WINDOW_MS = 2000; // 2秒内造成过伤害算作"进攻状态"
    private static final long DAMAGE_WINDOW_MS = 500;  // 500ms内受到过伤害算作"被反打"

    // --- 数据记录 ---
    private final Map<UUID, Long> lastPacketTime = new ConcurrentHashMap<>(); // 用于计算CPS
    private final Map<UUID, Long> lastSuccessfulHit = new ConcurrentHashMap<>(); // 上次造成实伤害的时间
    private final Map<UUID, Long> lastDamageTaken = new ConcurrentHashMap<>();   // 上次受到伤害的时间

    public SmoothCpsManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // 1. 注册 ProtocolLib 监听器 (拦截包)
        registerPacketListener();

        // 2. 注册 Bukkit 监听器 (监听伤害事件来判断Combo)
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        startCleanupTask();
    }

    /**
     * 判断玩家是否处于"连击" (Combo) 状态
     * 逻辑：最近造成了伤害 && 最近没有受到伤害
     */
    private boolean isComboing(Player player) {
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();

        // 1. 检查是否在进攻 (最近 2秒内有没有打中人)
        Long lastAttack = lastSuccessfulHit.get(uid);
        if (lastAttack == null || (now - lastAttack > ATTACK_WINDOW_MS)) {
            return false; // 没打中人，肯定不是 Combo
        }

        // 2. 检查是否在挨打 (最近 0.5秒内有没有被打)
        Long lastDamage = lastDamageTaken.get(uid);
        if (lastDamage != null && (now - lastDamage <= DAMAGE_WINDOW_MS)) {
            return false; // 刚被打了一下，说明是在"对刀"，不是单方面 Combo
        }

        // 既在进攻，又没怎么掉血 -> 正在 Combo！
        return true;
    }

    private void registerPacketListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.LOWEST, PacketType.Play.Client.USE_ENTITY) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        Player player = event.getPlayer();
                        if (player == null) return;

                        // 只处理左键攻击
                        try {
                            PacketContainer packet = event.getPacket();
                            EnumWrappers.EntityUseAction action = packet.getEntityUseActions().read(0);
                            if (action != EnumWrappers.EntityUseAction.ATTACK) return;
                        } catch (Exception e) { return; }

                        // --- 核心逻辑修改 ---

                        // 如果玩家正在 Combo 对手，放开限制，让他爽！
                        if (isComboing(player)) {
                            return;
                        }

                        // 否则 (在对刀、或者处于劣势)，开启 CPS 限制来修正击退
                        UUID uuid = player.getUniqueId();
                        long now = System.currentTimeMillis();
                        long lastTime = lastPacketTime.getOrDefault(uuid, 0L);

                        if (now - lastTime < MIN_DELAY_MS) {
                            // 点击太快，且不在 Combo 状态 -> 拦截！
                            event.setCancelled(true);
                        } else {
                            // 合法点击
                            lastPacketTime.put(uuid, now);
                        }
                    }
                }
        );
    }

    // --- Bukkit 事件监听 (用于更新 Combo 状态) ---

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDealDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            // 更新玩家"造成伤害"的时间
            lastSuccessfulHit.put(event.getDamager().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            // 更新玩家"受到伤害"的时间
            lastDamageTaken.put(event.getEntity().getUniqueId(), System.currentTimeMillis());
        }
    }

    // --- 清理任务 ---
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            lastPacketTime.entrySet().removeIf(e -> now - e.getValue() > 60000);
            lastSuccessfulHit.entrySet().removeIf(e -> now - e.getValue() > 60000);
            lastDamageTaken.entrySet().removeIf(e -> now - e.getValue() > 60000);
        }, 12000L, 12000L);
    }
}
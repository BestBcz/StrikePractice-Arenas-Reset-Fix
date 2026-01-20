package com.micet.arenareset;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

// --- StrikePractice Imports ---
import ga.strikepractice.StrikePractice;
import ga.strikepractice.battlekit.BattleKit;
import ga.strikepractice.events.BotDuelEndEvent;
import ga.strikepractice.events.DuelEndEvent;
import ga.strikepractice.events.KitDeselectEvent;
import ga.strikepractice.events.KitSelectEvent;
// ------------------------------

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*; // 引入并发包
import java.util.logging.Level;

public class MisplaceManager implements Listener {

    private final JavaPlugin plugin;
    public boolean isEnabled = true;

    private static final long ATTACK_WINDOW_MS = 2000;
    private static final long DAMAGE_WINDOW_MS = 500;

    // 缓存现在存储 Double
    private final Map<UUID, Double> playerKitDelayCache = new ConcurrentHashMap<>();

    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();

    //  当前延迟也是 Double
    private final Map<UUID, Double> currentDelayMap = new ConcurrentHashMap<>();
    // 设置也是 Double
    private final Map<String, Double> kitDelaySettings = new HashMap<>();

    //  毫秒级线程调度池
    private final ScheduledExecutorService packetScheduler = Executors.newScheduledThreadPool(4);

    private static final List<PacketType> MOVE_PACKETS = Arrays.asList(
            PacketType.Play.Server.REL_ENTITY_MOVE,
            PacketType.Play.Server.ENTITY_MOVE_LOOK,
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.ENTITY_LOOK
    );

    public MisplaceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupKitSettings();

        Bukkit.getPluginManager().registerEvents(this, plugin);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, MOVE_PACKETS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!isEnabled) return;
                handleMovePacket(event);
            }
        });

        // 1. 异步清理任务
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                lastAttackTime.values().removeIf(t -> now - t > 10000);
                lastDamageTime.values().removeIf(t -> now - t > 10000);

                Iterator<UUID> cacheIt = playerKitDelayCache.keySet().iterator();
                while (cacheIt.hasNext()) {
                    UUID uuid = cacheIt.next();
                    if (Bukkit.getPlayer(uuid) == null) {
                        cacheIt.remove();
                        currentDelayMap.remove(uuid);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 1200L, 1200L);

        // 2. 同步状态检查任务 (5秒一次)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isEnabled) return;
                for (World world : Bukkit.getWorlds()) {
                    for (Player player : world.getPlayers()) {
                        UUID uuid = player.getUniqueId();
                        if (playerKitDelayCache.containsKey(uuid)) continue;

                        if (StrikePractice.getAPI().getFight(player) != null) {
                            try {
                                Object kitObj = StrikePractice.getAPI().getKit(player);
                                if (kitObj instanceof BattleKit) {
                                    updatePlayerCache(player, (BattleKit) kitObj);
                                }
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    // 插件卸载时关闭线程池，防止内存泄漏
    public void onDisable() {
        packetScheduler.shutdownNow();
    }

    private void setupKitSettings() {
        kitDelaySettings.put("nodebuff", 1.4); //70ms
        kitDelaySettings.put("boxing", 2.3);   //115ms
        kitDelaySettings.put("builduhc", 0.0);
        kitDelaySettings.put("sumo", 0.0);
        kitDelaySettings.put("combo", 0.0);
        kitDelaySettings.put("gapple", 1.4);
        kitDelaySettings.put("diamond", 0.0);
        kitDelaySettings.put("enderpot", 1.4);
        kitDelaySettings.put("debuff", 1.4);
    }

    // [修改] 返回类型改为 double
    public double getPlayerDelay(Player player) {
        return playerKitDelayCache.getOrDefault(player.getUniqueId(), 0.0);
    }

    // --- 事件监听 ---

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKitSelect(KitSelectEvent event) {
        BattleKit kit = event.getKit();
        updatePlayerCache(event.getPlayer(), kit);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKitDeselect(KitDeselectEvent event) {
        clearPlayerCache(event.getPlayer());
    }

    @EventHandler
    public void onDuelEnd(DuelEndEvent event) {
        clearPlayerCache(event.getWinner());
        clearPlayerCache(event.getLoser());
    }

    @EventHandler
    public void onBotDuelEnd(BotDuelEndEvent event) {
        clearPlayerCache(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayerCache(event.getPlayer());
    }

    private void clearPlayerCache(Player player) {
        if (player != null) {
            playerKitDelayCache.remove(player.getUniqueId());
        }
    }

    private void updatePlayerCache(Player player, BattleKit kit) {
        if (kit == null) {
            playerKitDelayCache.remove(player.getUniqueId());
            return;
        }

        if (kit.isElo()) {
            playerKitDelayCache.put(player.getUniqueId(), 0.0);
            return;
        }

        String kitName = kit.getName().toLowerCase();
        double delay = kitDelaySettings.getOrDefault(kitName, 1.0); // 默认 1.0

        playerKitDelayCache.put(player.getUniqueId(), delay);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            lastAttackTime.put(event.getDamager().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            lastDamageTime.put(event.getEntity().getUniqueId(), System.currentTimeMillis());
        }
    }

    // --- 数据包处理 (毫秒级核心) ---

    private void handleMovePacket(PacketEvent event) {
        final Player receiver = event.getPlayer();
        int entityId = event.getPacket().getIntegers().read(0);

        Entity movingEntity = null;
        try {
            movingEntity = ProtocolLibrary.getProtocolManager().getEntityFromID(receiver.getWorld(), entityId);
        } catch (Exception e) {
            for(Entity ent : receiver.getWorld().getEntities()) {
                if(ent.getEntityId() == entityId) {
                    movingEntity = ent;
                    break;
                }
            }
        }

        if (movingEntity instanceof Player && movingEntity.getEntityId() != receiver.getEntityId()) {
            Player attacker = (Player) movingEntity;
            UUID attackerUUID = attacker.getUniqueId();

            // 获取目标延迟 (Double)
            double maxDelayForThisKit = playerKitDelayCache.getOrDefault(attackerUUID, 0.0);
            double targetDelay = isInComboState(attacker) ? maxDelayForThisKit : 0.0;

            // 平滑过渡
            double currentDelay = currentDelayMap.getOrDefault(attackerUUID, 0.0);
            double step = 0.25; // 每次发包变化 0.25 tick

            if (currentDelay < targetDelay) {
                currentDelay = Math.min(currentDelay + step, targetDelay);
            } else if (currentDelay > targetDelay) {
                currentDelay = Math.max(currentDelay - step, targetDelay);
            }
            currentDelayMap.put(attackerUUID, currentDelay);

            // 3. 执行延迟 (将 ticks 转换为 毫秒)
            if (currentDelay > 0.05) { // 忽略极小的延迟
                if (!event.isCancelled()) {
                    event.setCancelled(true);
                    final PacketContainer packet = event.getPacket().deepClone();

                    // 核心算法：Tick -> Milliseconds
                    // 1 Tick = 50ms
                    // 1.5 Ticks = 75ms
                    long delayMs = (long) (currentDelay * 50.0);

                    // 使用 Java 原生调度器替代 BukkitRunnable
                    packetScheduler.schedule(() -> {
                        try {
                            // 注意：这里是在异步线程发送包，ProtocolLib 支持线程安全发送
                            ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, packet, false);
                        } catch (InvocationTargetException e) {
                            plugin.getLogger().log(Level.WARNING, "Error sending misplaced packet", e);
                        }
                    }, delayMs, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private boolean isInComboState(Player player) {
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();

        Long lastAttack = lastAttackTime.get(uid);
        if (lastAttack == null || (now - lastAttack > ATTACK_WINDOW_MS)) {
            return false;
        }

        Long lastDamage = lastDamageTime.get(uid);
        if (lastDamage != null && (now - lastDamage <= DAMAGE_WINDOW_MS)) {
            return false;
        }

        return true;
    }
}
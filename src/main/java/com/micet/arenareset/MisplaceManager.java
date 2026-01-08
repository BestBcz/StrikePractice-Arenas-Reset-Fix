package com.micet.arenareset;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

// --- StrikePractice Imports ---
import ga.strikepractice.battlekit.BattleKit;
import ga.strikepractice.events.BotDuelEndEvent;
import ga.strikepractice.events.DuelEndEvent;
import ga.strikepractice.events.KitSelectEvent;
import ga.strikepractice.events.KitDeselectEvent;

import org.bukkit.Bukkit;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MisplaceManager implements Listener {

    private final JavaPlugin plugin;
    public boolean isEnabled = true;

    private static final long ATTACK_WINDOW_MS = 2000;
    private static final long DAMAGE_WINDOW_MS = 500;

    // UUID -> Delay Ticks
    private final Map<UUID, Integer> playerKitDelayCache = new ConcurrentHashMap<>();

    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> currentDelayMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> kitDelaySettings = new HashMap<>();

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

        // 异步清理任务
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                lastAttackTime.values().removeIf(t -> now - t > 10000);
                lastDamageTime.values().removeIf(t -> now - t > 10000);

                // 清理离线玩家数据
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
    }

    private void setupKitSettings() {
        kitDelaySettings.put("nodebuff", 1);
        kitDelaySettings.put("boxing", 2);
        kitDelaySettings.put("builduhc", 0);
        kitDelaySettings.put("sumo", 0);
        kitDelaySettings.put("combo", 0);
        kitDelaySettings.put("gapple", 1);
        kitDelaySettings.put("diamond", 1);
        kitDelaySettings.put("enderpot", 1);
        kitDelaySettings.put("debuff", 1);
    }

    // 获取指定玩家的 Misplace 延迟值 (供指令调用)
    public int getPlayerDelay(Player player) {
        return playerKitDelayCache.getOrDefault(player.getUniqueId(), 0);
    }

    //核心事件监听
    @EventHandler(priority = EventPriority.MONITOR)
    public void onKitSelect(KitSelectEvent event) {
        BattleKit kit = event.getKit();
        updatePlayerCache(event.getPlayer(), kit);
    }

    // 监听玩家取消 Kit 选择
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

    // 辅助方法：清除玩家缓存
    private void clearPlayerCache(Player player) {
        if (player != null) {
            playerKitDelayCache.remove(player.getUniqueId());
        }
    }

    //缓存更新逻辑

    private void updatePlayerCache(Player player, BattleKit kit) {
        if (kit == null) {
            playerKitDelayCache.remove(player.getUniqueId());
            return;
        }

        if (kit.isElo()) {
            playerKitDelayCache.put(player.getUniqueId(), 0);
            return;
        }

        String kitName = kit.getName().toLowerCase();
        int delay = kitDelaySettings.getOrDefault(kitName, 1);

        playerKitDelayCache.put(player.getUniqueId(), delay);
    }

    //战斗状态监听

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

    //数据包处理

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

            int maxDelayForThisKit = playerKitDelayCache.getOrDefault(attackerUUID, 0);
            int targetDelay = isInComboState(attacker) ? maxDelayForThisKit : 0;

            int currentDelay = currentDelayMap.getOrDefault(attackerUUID, 0);
            if (currentDelay < targetDelay) {
                currentDelay++;
            } else if (currentDelay > targetDelay) {
                currentDelay--;
            }
            currentDelayMap.put(attackerUUID, currentDelay);

            if (currentDelay > 0) {
                if (!event.isCancelled()) {
                    event.setCancelled(true);
                    final PacketContainer packet = event.getPacket().deepClone();

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, packet, false);
                            } catch (InvocationTargetException e) {
                                plugin.getLogger().log(Level.WARNING, "Error sending misplaced packet", e);
                            }
                        }
                    }.runTaskLater(plugin, currentDelay);
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
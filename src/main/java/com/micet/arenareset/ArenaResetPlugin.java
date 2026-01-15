package com.micet.arenareset;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.schematic.SchematicFormat;

import ga.strikepractice.arena.Arena;
import ga.strikepractice.events.*;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.micet.arenareset.HungerManager;
import java.io.File;

public class ArenaResetPlugin extends JavaPlugin implements Listener {
    private MisplaceManager misplaceManager;
    private SmoothCpsManager smoothCpsManager; // [新增]
    private WorldEditPlugin worldEdit;

    // 设置延迟时间 (4秒 = 80 ticks)
    private static final long RESET_DELAY_SECONDS = 5;

    @Override
    public void onEnable() {
        Plugin we = getServer().getPluginManager().getPlugin("WorldEdit");
        if (we instanceof WorldEditPlugin) {
            this.worldEdit = (WorldEditPlugin) we;
        } else {
            getLogger().severe("未找到 WorldEdit! 插件卸载。");
            setEnabled(false);
            return;
        }

        // 初始化饥饿控制模块
        new HungerManager(this);
        getLogger().info("饥饿控制模块已加载 (饥饿速度已减缓)");

        // 1. 初始化 Misplace 功能模块
        this.misplaceManager = new MisplaceManager(this);
        getLogger().info("Misplace 模块已加载！默认关闭，使用 /misplace 开启。");

        // 3. 初始化平滑 CPS 限制模块 (ProtocolLib)
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            // 构造函数里会自动注册 PacketListener 和 Bukkit Listener
            this.smoothCpsManager = new SmoothCpsManager(this);
            getLogger().info("SmoothCPS 模块已加载！(对刀时限制CPS，Combo时解除限制)");
        } else {
            getLogger().warning("未找到 ProtocolLib，SmoothCPS 模块无法启动！");
        }

        // 2. 注册指令
        getCommand("saveallarenas").setExecutor(new ArenaSaver(this));
        getCommand("tparena").setExecutor(new ArenaTeleporter());
        getCommand("misplace").setExecutor(new MisplaceCommand(this)); // [新增] 注册 Misplace 指令

        // 3. 注册监听器
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ArenaReset 已加载! (延迟重置 + Misplace集成版)");

        File schemDir = new File(getDataFolder(), "schematics");
        if (!schemDir.exists()) {
            schemDir.mkdirs();
        }
    }

    // [新增] 提供给 Command 类使用的方法
    public MisplaceManager getMisplaceManager() {
        return misplaceManager;
    }

    // ==========================================
    //            事件监听区域
    // ==========================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDuelEnd(DuelEndEvent event) {
        if (event.getFight() != null) scheduleReset(event.getFight().getArena());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBotDuelEnd(BotDuelEndEvent event) {
        if (event.getFight() != null) scheduleReset(event.getFight().getArena());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPartyVsPartyEnd(PartyVsPartyEndEvent event) {
        if (event.getFight() != null) scheduleReset(event.getFight().getArena());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPartySplitEnd(PartySplitEndEvent event) {
        if (event.getFight() != null) scheduleReset(event.getFight().getArena());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPartyFFAEnd(PartyFFAEndEvent event) {
        if (event.getFight() != null) scheduleReset(event.getFight().getArena());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPartyVsBotsEnd(PartyVsBotsEndEvent event) {
        if (event.getFight() != null) scheduleReset(event.getFight().getArena());
    }

    // ==========================================
    //            核心延迟重置逻辑
    // ==========================================

    private void scheduleReset(final Arena arena) {
        if (arena == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                performReset(arena);
            }
        }.runTaskLater(this, RESET_DELAY_SECONDS * 20L); // 修正: 20 ticks = 1秒，之前写的25L有点奇怪
    }

    private void performReset(Arena arena) {
        String arenaName = arena.getName();
        String templateName;

        if (arenaName.contains(":")) {
            templateName = arenaName.substring(arenaName.lastIndexOf(":") + 1);
        } else {
            templateName = arenaName;
        }

        File schemFile = new File(getDataFolder() + "/schematics", templateName + ".schematic");

        if (!schemFile.exists()) {
            String fallbackName = arenaName.replace(":", "_");
            File fallbackFile = new File(getDataFolder() + "/schematics", fallbackName + ".schematic");

            if (fallbackFile.exists()) {
                schemFile = fallbackFile;
            } else {
                return;
            }
        }

        Location pasteLoc = arena.getLoc1();
        if (pasteLoc != null && pasteLoc.getWorld() != null) {
            pasteSchematic(schemFile, pasteLoc);
        }
    }

    private void pasteSchematic(File file, Location loc) {
        try {
            SchematicFormat format = SchematicFormat.getFormat(file);
            if (format == null) format = SchematicFormat.MCEDIT;

            CuboidClipboard clipboard = format.load(file);
            EditSession session = worldEdit.getWorldEdit().getEditSessionFactory().getEditSession(new BukkitWorld(loc.getWorld()), -1);

            clipboard.paste(session, new Vector(loc.getX(), loc.getY(), loc.getZ()), false);

        } catch (Exception e) {
            e.printStackTrace();
            getLogger().warning("重置竞技场时出错: " + file.getName());
        }
    }
}
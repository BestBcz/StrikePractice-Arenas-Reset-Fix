package com.micet.arenareset;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.schematic.SchematicFormat;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.events.DuelEndEvent;
import ga.strikepractice.arena.Arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ArenaResetPlugin extends JavaPlugin implements Listener {

    private WorldEditPlugin worldEdit;

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
        getCommand("tparena").setExecutor(new ArenaTeleporter());
        // 依然保留 saveall 指令，虽然你现在用手动保存了，但留着也没坏处
        getCommand("saveallarenas").setExecutor(new ArenaSaver(this));

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ArenaReset 已加载! (智能模板映射版)");

        File schemDir = new File(getDataFolder(), "schematics");
        if (!schemDir.exists()) {
            schemDir.mkdirs();
        }
    }

    @EventHandler
    public void onDuelEnd(DuelEndEvent event) {
        if (event.getFight() == null || event.getFight().getArena() == null) return;

        Arena arena = event.getFight().getArena();
        String arenaName = arena.getName(); // 例如 "Arenas_1:1" 或 "Arenas_2:sumo10"

        // --- 智能解析文件名 ---
        String templateName;

        // 如果名字里包含冒号 (说明是克隆体)
        if (arenaName.contains(":")) {
            // 提取冒号后面的部分。例如 "Arenas_1:1" -> "1"
            templateName = arenaName.substring(arenaName.lastIndexOf(":") + 1);
        } else {
            // 如果没有冒号，就用原名
            templateName = arenaName;
        }

        // 寻找对应的 Schematic 文件 (例如 1.schematic)
        File schemFile = new File(getDataFolder() + "/schematics", templateName + ".schematic");

        if (!schemFile.exists()) {
            // 如果找不到 1.schematic，尝试找一下全名 Arenas_1_1.schematic (作为备用方案)
            String fallbackName = arenaName.replace(":", "_");
            File fallbackFile = new File(getDataFolder() + "/schematics", fallbackName + ".schematic");

            if (fallbackFile.exists()) {
                schemFile = fallbackFile;
            } else {
                // 如果都找不到，那就没办法了
                // getLogger().warning("未找到模板文件: " + templateName + ".schematic");
                return;
            }
        }

        // 获取粘贴位置
        Location pasteLoc = arena.getLoc1();

        // 执行粘贴
        pasteArena(schemFile, pasteLoc);
    }

    private void pasteArena(File file, Location loc) {
        try {
            SchematicFormat format = SchematicFormat.getFormat(file);
            if (format == null) format = SchematicFormat.MCEDIT; // 强制兼容

            CuboidClipboard clipboard = format.load(file);
            EditSession session = worldEdit.getWorldEdit().getEditSessionFactory().getEditSession(new BukkitWorld(loc.getWorld()), -1);

            // 粘贴 (false = 不忽略空气，覆盖掉玩家放的方块)
            clipboard.paste(session, new Vector(loc.getX(), loc.getY(), loc.getZ()), false);

            getLogger().info("已重置: " + file.getName()); // 觉得刷屏可以注释掉

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package com.micet.arenareset;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.schematic.SchematicFormat;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.arena.Arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;

public class ArenaSaver implements CommandExecutor {
      //BROKEN!!!!!!!!!!!!!!!
      //BROKEN!!!!!!!!!!!!!!!
      //BROKEN!!!!!!!!!!!!!!!
      //BROKEN!!!!!!!!!!!!!!!
      //BROKEN!!!!!!!!!!!!!!!
      //BROKEN!!!!!!!!!!!!!!!
    private final JavaPlugin plugin;

    public ArenaSaver(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arenareset.admin")) {
            sender.sendMessage("§cno permission。");
            return true;
        }

        StrikePracticeAPI api = StrikePractice.getAPI();
        List<Arena> arenas = api.getArenas();

        if (arenas == null || arenas.isEmpty()) {
            sender.sendMessage("§cNot found any arena data！");
            return true;
        }

        sender.sendMessage("§eStart saving " + arenas.size() + " arenas...");
        sender.sendMessage("§7(Relocating pos，sure your paste is right!)");

        WorldEditPlugin we = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (we == null) {
            sender.sendMessage("§cError：Not Found WorldEdit Plugin！");
            return true;
        }

        File saveDir = new File(plugin.getDataFolder(), "schematics");
        if (!saveDir.exists()) saveDir.mkdirs();

        new ArenaSaveTask(sender, arenas, we, saveDir).runTaskTimer(plugin, 1L, 10L);

        return true;
    }

    private class ArenaSaveTask extends BukkitRunnable {
        private final CommandSender sender;
        private final List<Arena> queue;
        private final WorldEditPlugin we;
        private final File saveDir;

        private int index = 0;
        private int success = 0;
        private int fail = 0;

        public ArenaSaveTask(CommandSender sender, List<Arena> queue, WorldEditPlugin we, File saveDir) {
            this.sender = sender;
            this.queue = queue;
            this.we = we;
            this.saveDir = saveDir;
        }

        @Override
        public void run() {
            if (index >= queue.size()) {
                sender.sendMessage("§aAll Done！");
                sender.sendMessage("§7success: §a" + success + " §7fail: §c" + fail);
                this.cancel();
                return;
            }

            Arena arena = queue.get(index);
            index++;

            try {
                Location loc1 = arena.getLoc1();
                Location loc2 = arena.getLoc2();

                if (loc1 == null || loc2 == null || loc1.getWorld() == null) {
                    plugin.getLogger().warning("跳过无效竞技场: " + arena.getName());
                    fail++;
                    return;
                }

                // 文件名去冒号处理
                String safeName = arena.getName().replace(":", "_");
                File schemFile = new File(saveDir, safeName + ".schematic");

                saveSchematic(we, loc1, loc2, schemFile);

                success++;
                sender.sendMessage("§7[§e" + index + "/" + queue.size() + "§7] 已保存: §f" + safeName);

            } catch (Exception e) {
                sender.sendMessage("§cError saving: " + arena.getName());
                plugin.getLogger().warning("Error saving " + arena.getName() + ": " + e.toString());
                e.printStackTrace();
                fail++;
            }
        }
    }

    private void saveSchematic(WorldEditPlugin we, Location l1, Location l2, File file) throws Exception {
        BukkitWorld weWorld = new BukkitWorld(l1.getWorld());

        Vector min = new Vector(Math.min(l1.getBlockX(), l2.getBlockX()), Math.min(l1.getBlockY(), l2.getBlockY()), Math.min(l1.getBlockZ(), l2.getBlockZ()));
        Vector max = new Vector(Math.max(l1.getBlockX(), l2.getBlockX()), Math.max(l1.getBlockY(), l2.getBlockY()), Math.max(l1.getBlockZ(), l2.getBlockZ()));
        Vector size = max.subtract(min).add(1, 1, 1);

        EditSession session = we.getWorldEdit().getEditSessionFactory().getEditSession(weWorld, -1);

        CuboidClipboard clipboard = new CuboidClipboard(size, min);
        clipboard.copy(session); // 读取数据

        Vector loc1Vec = new Vector(l1.getBlockX(), l1.getBlockY(), l1.getBlockZ());
        clipboard.setOffset(min.subtract(loc1Vec));

        SchematicFormat format = SchematicFormat.getFormat("schematic");
        if (format == null) format = SchematicFormat.MCEDIT;

        format.save(clipboard, file);
    }
}
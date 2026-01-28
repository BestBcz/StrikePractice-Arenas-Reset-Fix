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

import java.io.File;

public class ArenaResetPlugin extends JavaPlugin implements Listener {

    private WorldEditPlugin worldEdit;

    // RESET_DELAY_SECONDS
    private static final long RESET_DELAY_SECONDS = 5;

    @Override
    public void onEnable() {
        Plugin we = getServer().getPluginManager().getPlugin("WorldEdit");
        if (we instanceof WorldEditPlugin) {
            this.worldEdit = (WorldEditPlugin) we;
        } else {
            getLogger().severe("WorldEdit Not Found! Error!!!");
            setEnabled(false);
            return;
        }

        getCommand("saveallarenas").setExecutor(new ArenaSaver(this));
        getCommand("tparena").setExecutor(new ArenaTeleporter());

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ArenaReset Loaded!");

        File schemDir = new File(getDataFolder(), "schematics");
        if (!schemDir.exists()) {
            schemDir.mkdirs();
        }
    }


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

    private void scheduleReset(final Arena arena) {
        if (arena == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                performReset(arena);
            }
        }.runTaskLater(this, RESET_DELAY_SECONDS * 20L); // 1s
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
            getLogger().warning("Error when reset arena: " + file.getName());
        }
    }
    @Override
    public void onDisable() {
    }
}
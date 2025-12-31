package com.micet.arenareset;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.arena.Arena;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaTeleporter implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        if (args.length == 0) return false;

        Player p = (Player) sender;
        String arenaName = args[0];

        // 获取竞技场
        Arena arena = StrikePractice.getAPI().getArena(arenaName);
        if (arena == null) {
            p.sendMessage(ChatColor.RED + "找不到竞技场: " + arenaName);
            return true;
        }

        Location loc1 = arena.getLoc1();
        if (loc1 == null) {
            p.sendMessage(ChatColor.RED + "该竞技场没有 Loc1 数据！");
            return true;
        }

        // 传送到 Loc1
        p.teleport(loc1);
        p.sendMessage(ChatColor.GREEN + "已传送至 " + arenaName + " 的 Loc1 (锚点)。");
        p.sendMessage(ChatColor.YELLOW + "请站在这里不要动，选区后输入 //copy 然后 //schem save " + arenaName);

        return true;
    }
}
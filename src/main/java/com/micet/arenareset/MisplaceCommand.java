package com.micet.arenareset;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MisplaceCommand implements CommandExecutor {

    private final ArenaResetPlugin plugin;

    public MisplaceCommand(ArenaResetPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arenareset.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return true;
        }

        MisplaceManager manager = plugin.getMisplaceManager();

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "=== Misplace 控制台 ===");
            sender.sendMessage(ChatColor.GOLD + "/misplace on " + ChatColor.WHITE + "- 全局开启");
            sender.sendMessage(ChatColor.GOLD + "/misplace off " + ChatColor.WHITE + "- 全局关闭");
            sender.sendMessage(ChatColor.GOLD + "/misplace <玩家名> " + ChatColor.WHITE + "- 查看玩家当前Misplace值");
            sender.sendMessage(ChatColor.GRAY + "当前全局状态: " + (manager.isEnabled ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭"));
            return true;
        }

        String subCommand = args[0];

        if (subCommand.equalsIgnoreCase("off")) {
            manager.isEnabled = false;
            sender.sendMessage(ChatColor.YELLOW + "[Misplace] 功能已全局关闭。");
            return true;
        }

        if (subCommand.equalsIgnoreCase("on")) {
            manager.isEnabled = true;
            sender.sendMessage(ChatColor.GREEN + "[Misplace] 已全局开启！");
            sender.sendMessage(ChatColor.GRAY + "说明: 延迟数值根据玩家所在的 Kit 自动调整。");
            return true;
        }

        // 尝试作为玩家名进行查询
        Player target = Bukkit.getPlayer(subCommand);
        if (target != null) {
            // [修正] 这里改为 double 以匹配 Manager 的返回值
            double delay = manager.getPlayerDelay(target);

            sender.sendMessage(ChatColor.GREEN + "查询结果: " + ChatColor.WHITE + target.getName());
            if (delay > 0) {
                // [优化] 输出时保留一位小数，例如 1.5 ticks
                sender.sendMessage(ChatColor.GREEN + "当前 Misplace 设置: " + ChatColor.YELLOW + String.format("%.1f", delay) + " ticks");
                sender.sendMessage(ChatColor.GRAY + "(注意: 此数值仅在连击/Combo状态下生效)");
            } else {
                sender.sendMessage(ChatColor.RED + "当前 Misplace 设置: 0.0 (未开启/无Kit/排位模式)");
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "未知指令或玩家不在线。用法: /misplace [on/off/玩家ID]");
        return true;
    }
}
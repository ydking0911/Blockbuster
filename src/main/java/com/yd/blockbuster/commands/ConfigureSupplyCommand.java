package com.yd.blockbuster.commands;

import com.yd.blockbuster.managers.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ConfigureSupplyCommand implements CommandExecutor {

    private final GameManager gameManager;

    public ConfigureSupplyCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "보급품을 설정할 권한이 없습니다.");
            return true;
        }

        gameManager.openSupplyConfigGUI(player);
        return true;
    }
}
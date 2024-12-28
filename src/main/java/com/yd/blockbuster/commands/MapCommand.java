package com.yd.blockbuster.commands;

import com.yd.blockbuster.managers.GameManager;
import com.yd.blockbuster.managers.SelectionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class MapCommand implements CommandExecutor {

    private final GameManager gameManager;

    public MapCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "이 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        gameManager.restoreGameAreaState();
        sender.sendMessage(ChatColor.GREEN + "맵을 초기화했습니다.");
        return true;
    }
}
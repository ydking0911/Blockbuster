package com.yd.blockbuster.commands;

import com.yd.blockbuster.managers.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class EndGameCommand implements CommandExecutor {

    private final GameManager gameManager;

    public EndGameCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "게임을 종료할 권한이 없습니다.");
            return true;
        }

        gameManager.endGame();
        sender.sendMessage(ChatColor.GREEN + "게임이 종료되었습니다.");

        return true;
    }
}
package com.yd.blockbuster.commands;

import com.yd.blockbuster.managers.GameManager;
import com.yd.blockbuster.managers.SelectionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class StartGameCommand implements CommandExecutor {

    private final GameManager gameManager;
    private final SelectionManager selectionManager;

    public StartGameCommand(GameManager gameManager, SelectionManager selectionManager) {
        this.gameManager = gameManager;
        this.selectionManager = selectionManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!commandSender.isOp()) {
            commandSender.sendMessage(ChatColor.RED + "게임을 시작할 권한이 없습니다.");
            return true;
        }

        if (!selectionManager.isSelectionComplete()) {
            commandSender.sendMessage(ChatColor.RED + "영역이 지정되지 않았습니다. 나무도끼로 영역을 지정해주세요.");
            return true;
        }

        gameManager.startGame(selectionManager.getSelection());
        commandSender.sendMessage(ChatColor.GREEN + "게임이 시작되었습니다!");


        return false;
    }
}

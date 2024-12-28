package com.yd.blockbuster.listeners;

import com.yd.blockbuster.managers.GameManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final GameManager gameManager;

    public PlayerListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.handlePlayerQuit(event);
    }

}

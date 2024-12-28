package com.yd.blockbuster.listeners;

import com.yd.blockbuster.managers.GameManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    private final GameManager gameManager;

    public GUIListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        gameManager.handleInventoryClick(event);
    }
}
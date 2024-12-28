package com.yd.blockbuster.listeners;

import com.yd.blockbuster.managers.GameManager;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class GameListener implements Listener {

    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        gameManager.handlePlayerDeath(event);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        gameManager.handleBlockBreak(event);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        gameManager.handleBedInteract(event);
        gameManager.handlePlayerInteract(event);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        gameManager.handleBlockPlace(event);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        gameManager.handleItemSpawn(event);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        gameManager.handleItemPickup(event);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        gameManager.handlePlayerMove(event);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        gameManager.handlePlayerRespawn(event);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        gameManager.handleProjectileHit(event);
    }

    @EventHandler
    public void onPlayerShootCrossbow(EntityShootBowEvent event) {
        gameManager.handlePlayerShootCrossbow(event);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        gameManager.handleEntityExplode(event);
    }

}

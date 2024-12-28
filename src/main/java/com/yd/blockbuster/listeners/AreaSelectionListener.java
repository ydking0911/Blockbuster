package com.yd.blockbuster.listeners;

import com.yd.blockbuster.managers.SelectionManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class AreaSelectionListener implements Listener {

    private final SelectionManager selectionManager;

    public AreaSelectionListener(SelectionManager selectionManager) {
        this.selectionManager = selectionManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        if (player.getInventory().getItemInMainHand().getType() != Material.WOODEN_AXE) return;

        Action action = event.getAction();
        Location location = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null;

        if (location == null) return;

        if (action == Action.LEFT_CLICK_BLOCK) {
            selectionManager.setPos1(location);
            player.sendMessage(ChatColor.YELLOW + "첫 번째 위치가 설정되었습니다: " + formatLocation(location));
            event.setCancelled(true);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            selectionManager.setPos2(location);
            player.sendMessage(ChatColor.YELLOW + "두 번째 위치가 설정되었습니다: " + formatLocation(location));
            event.setCancelled(true);
        }
    }

    private String formatLocation(Location loc) {
        return ChatColor.AQUA + "[" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]" + ChatColor.YELLOW;
    }

}

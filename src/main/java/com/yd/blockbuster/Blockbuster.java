package com.yd.blockbuster;

import com.yd.blockbuster.commands.ConfigureSupplyCommand;
import com.yd.blockbuster.commands.EndGameCommand;
import com.yd.blockbuster.commands.MapCommand;
import com.yd.blockbuster.commands.StartGameCommand;
import com.yd.blockbuster.listeners.AreaSelectionListener;
import com.yd.blockbuster.listeners.GUIListener;
import com.yd.blockbuster.listeners.GameListener;
import com.yd.blockbuster.listeners.PlayerListener;
import com.yd.blockbuster.managers.GameManager;
import com.yd.blockbuster.managers.SelectionManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Blockbuster extends JavaPlugin {

    private static Blockbuster instance;
    private GameManager gameManager;
    private SelectionManager selectionManager;

    @Override
    public void onEnable() {
        instance = this;
        gameManager = new GameManager(this);
        selectionManager = new SelectionManager();

        getCommand("게임시작").setExecutor(new StartGameCommand(gameManager,selectionManager));
        getCommand("게임끝").setExecutor(new EndGameCommand(gameManager));
        getCommand("config").setExecutor(new ConfigureSupplyCommand(gameManager));
        getCommand("맵초기화").setExecutor(new MapCommand(gameManager));

        getServer().getPluginManager().registerEvents(new AreaSelectionListener(selectionManager), this);
        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new GUIListener(gameManager), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Blockbuster getInstance() {
        return instance;
    }
}

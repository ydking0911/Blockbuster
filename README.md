# Blockbuster Minecraft Plugin

Blockbuster is a Bukkit/Spigot based plugin that provides a dynamic survival game mode for Minecraft servers.  
Players engage in an exciting game within a designated area featuring supply drops, TNT bombardments, bed protection and destruction events.

## Key Features

- **Area Selection**  
  Use a wooden axe to select two points and define the game area.
  - **Left-click**: Set the first position (pos1)
  - **Right-click**: Set the second position (pos2)

- **Game Start and End**  
  Operators (ops) can start or end the game via commands.
  - `/게임시작`: Start the game (requires a complete area selection).  
    When the game starts, all online players receive basic items and a bed, and the current state of the game area is saved.
  - `/게임끝`: End the game in progress.

- **Supply Drops and Configuration**  
  - Supply chests containing regular and special supply items (such as an explosive crossbow) are periodically spawned at the center of the game area.
  - `/config`: Opens an in-game GUI for configuring supply items (player command; op only).

- **Map Reset**  
  - `/맵초기화`: Restores the game area to its previously saved state.

- **TNT Bombardment**  
  - After a set period, TNT is dropped at random locations within the game area from the sky, intensifying the challenge.

- **Bed Management**  
  - Players use beds to set their respawn points.  
  - If a bed is destroyed, a warning is sent and the affected player is eliminated from the game.

## Installation

1. **Build the Plugin**  
   Build the project using Maven or Gradle to generate the `.jar` file.

2. **Install on Server**  
   Place the generated `.jar` file into the `plugins` folder of your Minecraft server.

3. **Restart the Server**  
   Restart the server to ensure the plugin is loaded properly.

## Commands

```bash
# Start the game (requires a complete area selection)
/게임시작

# End the game
/게임끝

# Configure supply items (player command; op only)
/config

# Reset the map (restore game area state)
/맵초기화

package com.yd.blockbuster.managers;

import com.yd.blockbuster.Blockbuster;
import com.yd.blockbuster.models.Selection;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameManager {

    private final Blockbuster plugin;
    private final Set<UUID> gamePlayers = new HashSet<>();
    private final Map<UUID, Integer> countdownTasks = new HashMap<>();
    private boolean gameStarted = false;
    private World gameWorld;
    private Selection gameArea;
    private Location centerLocation;
    private int supplyTaskId;

    // 보급품 아이템 목록
    private final List<ItemStack> supplyItems = new ArrayList<>();
    private final List<ItemStack> specialSupplyItems = new ArrayList<>();

    // 플레이어의 침대 위치를 추적하기 위한 맵
    private final Map<Location, UUID> bedLocations = new HashMap<>();

    // 맵 상태 백업용 자료구조
    private List<BlockState> originalMapState = new ArrayList<>();

    // 폭격 관련 변수
    private int bombardmentTaskId = -1;
    private int tntCount = 5;        // 폭탄 시작 개수 (초기값:10)
    private final int MAX_TNT = 320; // 한 번에 떨어질 최대 폭탄 개수

    public GameManager(Blockbuster plugin) {
        this.plugin = plugin;

        // 기본 보급품 아이템 초기화
        initializeDefaultSupplyItems();
    }

    private void initializeDefaultSupplyItems() {
        // Explosive Crossbow
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        CrossbowMeta crossbowMeta = (CrossbowMeta) crossbow.getItemMeta();

        ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET);
        FireworkMeta fireworkMeta = (FireworkMeta) firework.getItemMeta();
        fireworkMeta.addEffect(FireworkEffect.builder()
                .withColor(Color.RED)
                .withFade(Color.ORANGE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build());
        fireworkMeta.setPower(1);
        firework.setItemMeta(fireworkMeta);

        crossbowMeta.addChargedProjectile(firework);
        crossbow.setItemMeta(crossbowMeta);

        ItemStack specialCrossbow = new ItemStack(Material.CROSSBOW);
        CrossbowMeta specialCrossbowMeta = (CrossbowMeta) specialCrossbow.getItemMeta();

        ItemStack specialFirework = new ItemStack(Material.FIREWORK_ROCKET);
        FireworkMeta specialFireworkMeta = (FireworkMeta) specialFirework.getItemMeta();
        specialFireworkMeta.addEffect(FireworkEffect.builder()
                .withColor(Color.RED)
                .withFade(Color.ORANGE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build());
        specialFireworkMeta.setPower(1);
        specialFirework.setItemMeta(specialFireworkMeta);

        // 폭죽을 충전
        specialCrossbowMeta.addChargedProjectile(specialFirework);

        // 특수한 이름과 설명 추가
        specialCrossbowMeta.setDisplayName(ChatColor.DARK_RED + "폭발 쇠뇌");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "사용 시 5x5x5 폭발 발생");
        specialCrossbowMeta.setLore(lore);

        specialCrossbow.setItemMeta(specialCrossbowMeta);

        // 특별 보급품 아이템 목록에 추가
        specialSupplyItems.add(specialCrossbow);

        ItemStack bedLocator = new ItemStack(Material.COMPASS);
        ItemMeta meta = bedLocator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "적 침대 추적기");
            bedLocator.setItemMeta(meta);
        }
        supplyItems.add(bedLocator);

        // Golden Apple
        ItemStack goldenApple = new ItemStack(Material.GOLDEN_APPLE, 3);

        // Add to supply items list
        supplyItems.add(crossbow);
        supplyItems.add(goldenApple);
    }

    public void startGame(Selection selection) {
        this.gameWorld = selection.getPos1().getWorld();
        this.gameArea = selection;
        this.centerLocation = selection.getCenter();

        // 맵 상태 백업
        saveGameAreaState();

        for (Player player : Bukkit.getOnlinePlayers()) {
            gamePlayers.add(player.getUniqueId());
            setupPlayer(player);

            player.sendTitle(ChatColor.GREEN + "게임이 시작되었습니다!", ChatColor.YELLOW + "Made by Moody", 10, 40, 10);
        }
        gameStarted = true;

        // 보급품 생성 스케줄러 (10분 후)
        supplyTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::spawnSupplyDrop, 20L * 600); //600

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::spawnSpecialSupplyDrop, 20L * 1200); //1200

        // 15분(900초) 후 폭격 시작
        Bukkit.getScheduler().runTaskLater(plugin, this::startBombardment, 20L * 900); //20L*900

        Bukkit.broadcastMessage(ChatColor.GREEN + "게임이 시작되었습니다! 각자 침대를 숨기고 보호하세요.");
    }

    public void endGame() {
        if (!gameStarted) {
            return; // 게임이 이미 종료된 경우
        }

        // 스케줄된 작업 취소
        Bukkit.getScheduler().cancelTasks(plugin);

        // 게임 상태 초기화
        gameStarted = false;
        countdownTasks.clear();
        bedLocations.clear();

        // 플레이어 초기화
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 관전자 모드인 경우 서바이벌 모드로 변경
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SURVIVAL);
            }

            // 게임에 참여했던 플레이어인지 확인
            if (gamePlayers.contains(player.getUniqueId())) {
                player.getInventory().clear();
                player.teleport(gameWorld.getSpawnLocation());
            }
        }

        // 게임에 참여한 플레이어 목록 초기화
        gamePlayers.clear();

        Bukkit.broadcastMessage(ChatColor.YELLOW + "게임이 종료되었습니다.");
    }

    private void setupPlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        giveStarterItems(player);

        // 침대 지급
        player.getInventory().addItem(new ItemStack(Material.RED_BED));
    }

    private void spawnSupplyDrop() {
        // 보급품 위치를 설정합니다.
        int supplyX = 0;
        int supplyZ = 0;

        // 해당 위치에서 가장 높은 블록을 가져옵니다.
        Block highestBlock = gameWorld.getHighestBlockAt(supplyX, supplyZ);

        // 가장 높은 블록의 y좌표를 얻습니다.
        int highestY = highestBlock.getY();

        // 보급품 위치를 설정합니다 (블록 위에 상자를 놓기 위해 y좌표에 1을 더합니다).
        Location supplyLocation = new Location(gameWorld, supplyX + 0.5, highestY + 1, supplyZ + 0.5);

        // 해당 위치에 상자를 생성합니다.
        Block block = supplyLocation.getBlock();
        block.setType(Material.CHEST);

        // 상자의 인벤토리에 보급품 아이템을 추가합니다.
        BlockState state = block.getState();
        if (state instanceof Chest) {
            Chest chest = (Chest) state;
            Inventory chestInventory = chest.getBlockInventory();

            // 보급품 아이템을 상자에 넣습니다.
            for (ItemStack item : supplyItems) {
                chestInventory.addItem(item);
            }
        }

        Bukkit.broadcastMessage(ChatColor.GOLD + "중앙에 보급품 상자가 생성되었습니다!");
    }

    private void spawnSpecialSupplyDrop() {
        // 보급품 위치를 설정합니다.
        int supplyX = 0;
        int supplyZ = 0;

        // 해당 위치에서 가장 높은 블록을 가져옵니다.
        Block highestBlock = gameWorld.getHighestBlockAt(supplyX, supplyZ);
        int highestY = highestBlock.getY();

        // 보급품 위치를 설정합니다 (블록 위에 상자를 놓기 위해 y좌표에 1을 더합니다).
        Location supplyLocation = new Location(gameWorld, supplyX + 0.5, highestY + 1, supplyZ + 0.5);

        // 해당 위치에 상자를 생성합니다.
        Block block = supplyLocation.getBlock();
        block.setType(Material.CHEST);

        // 상자의 인벤토리에 특별 보급품 아이템을 추가합니다.
        BlockState state = block.getState();
        if (state instanceof Chest) {
            Chest chest = (Chest) state;
            Inventory chestInventory = chest.getBlockInventory();

            // 특별 보급품 아이템을 상자에 넣습니다.
            for (ItemStack item : specialSupplyItems) {
                chestInventory.addItem(item);
            }
        }

        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "중앙에 특별 보급품 상자가 생성되었습니다!");
    }

    public void handlePlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (gameStarted && gamePlayers.contains(player.getUniqueId())) {
            if (bedExistsForPlayer(player)) {
                // 침대 위치 기준으로 안전한 스폰 위치 찾기
                Location bedSpawn = player.getBedSpawnLocation();
                if (bedSpawn == null) {
                    // 혹시 침대 스폰 로케이션을 가져올 수 없는 경우, centerLocation 사용
                    bedSpawn = centerLocation.clone();
                }
                Location safeLocation = getSafeSpawnLocation(bedSpawn);
                event.setRespawnLocation(safeLocation);
                giveStarterItems(player);
                player.sendMessage(ChatColor.GREEN + "재생성되었습니다. 시작 아이템이 지급되었습니다.");
            } else {
                eliminatePlayer(player);
            }

            player.setGlowing(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.setGlowing(false), 20L*5); // 2초(40틱) 후 발광 제거
        }

    }

    public void handlePlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 나침반인지 확인 및 이름 비교
        if (item.getType() == Material.COMPASS &&
                item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals(ChatColor.RED + "적 침대 추적기")) {

            // 적 침대 좌표를 찾음
            boolean found = false;
            for (Map.Entry<Location, UUID> entry : bedLocations.entrySet()) {
                Location bedLocation = entry.getKey();
                UUID ownerUUID = entry.getValue();

                // 적인지 확인
                if (!ownerUUID.equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.YELLOW + "적 침대 위치: " +
                            ChatColor.AQUA + "X: " + bedLocation.getBlockX() +
                            " Y: " + bedLocation.getBlockY() +
                            " Z: " + bedLocation.getBlockZ());

                    // 나침반 방향 설정
                    player.setCompassTarget(bedLocation);
                    found = true;
                    break;
                }
            }

            if (!found) {
                player.sendMessage(ChatColor.RED + "추적할 적의 침대가 없습니다!");
            }

            // 나침반 사용 이벤트 소모 처리
            event.setCancelled(true);
        }
    }

    /**
     * 침대 주변 상공으로 플레이어가 스폰 가능한 위치를 반환하는 메서드
     */
    private Location getSafeSpawnLocation(Location baseLocation) {
        World world = baseLocation.getWorld();
        int x = baseLocation.getBlockX();
        int y = baseLocation.getBlockY();
        int z = baseLocation.getBlockZ();

        // y를 위로 올려가며 공기 블록인 곳을 찾는다.
        // 특정 범위 제한 (예: 10블록 상공까지만)
        for (int i = 0; i < 10; i++) {
            Block block = world.getBlockAt(x, y + i, z);
            Block blockAbove = world.getBlockAt(x, y + i + 1, z);
            if (block.getType() == Material.AIR && blockAbove.getType() == Material.AIR) {
                return new Location(world, x + 0.5, y + i, z + 0.5);
            }
        }

        // 만약 적절한 공간을 못 찾을 경우 기본 위치 반환
        return baseLocation.add(0.5, 0, 0.5);
    }

    public void handleBedInteract(PlayerInteractEvent event) {
        if (!gameStarted) return;

        Action action = event.getAction();
        Block clickedBlock = event.getClickedBlock();

        // 플레이어가 우클릭했는지 확인하고, 클릭한 블록이 침대인지 확인
        if (action == Action.RIGHT_CLICK_BLOCK && clickedBlock != null && clickedBlock.getType().toString().endsWith("_BED")) {
            // 이벤트 취소하여 기본 동작 방지 (리스폰 지점 설정 방지)
            event.setCancelled(true);
        }
    }

    public void handleProjectileHit(ProjectileHitEvent event) {
        // 발사체가 폭죽인지 확인
        if (event.getEntity() instanceof Firework) {
            Firework firework = (Firework) event.getEntity();

            // 폭죽이 석궁에서 발사된 것인지 확인
            if (firework.hasMetadata("launchedFromCrossbow")) {
                // 폭죽이 블록에 충돌했는지 확인
                if (event.getHitBlock() != null) {
                    Location hitLocation = event.getHitBlock().getLocation();

                    // 게임 영역 내인지 확인
                    if (!gameArea.isInside(hitLocation)) {
                        return; // 게임 영역 밖이면 처리하지 않음
                    }

                    // 4x4x4 범위의 블록 삭제
                    removeBlocksAround(hitLocation, 2); // 반지름 2 (총 5x5x5 범위)

                    // 폭발 이펙트 생성 (폭발 파티클 표시)
                    hitLocation.getWorld().createExplosion(hitLocation, 2.0F, false, false);
                }
            }
        }
    }

    public void handlePlayerShootCrossbow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player && event.getBow().getType() == Material.CROSSBOW) {
            if (event.getProjectile() instanceof Firework) {
                Firework firework = (Firework) event.getProjectile();

                // 메타데이터 추가하여 석궁에서 발사된 폭죽임을 표시
                firework.setMetadata("launchedFromCrossbow", new FixedMetadataValue(Blockbuster.getInstance(), true));
            }
        }
    }

    private void removeBlocksAround(Location center, int radius) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    Location loc = new Location(world, x, y, z);
                    Block block = loc.getBlock();

                    // 블록 제거 (공기 블록은 제외)
                    if (block.getType() != Material.AIR) {
                        // 침대인지 확인
                        if (block.getType().toString().endsWith("_BED")) {
                            // 침대 파괴 처리
                            handleBedDestruction(block);
                        }
                        // 블록 파괴
                        block.breakNaturally();
                    }
                }
            }
        }
    }

    private void handleBedDestruction(Block block) {
        Location bedLocation = block.getLocation();

        if (bedLocations.containsKey(bedLocation)) {
            UUID playerUUID = bedLocations.get(bedLocation);
            Player bedOwner = Bukkit.getPlayer(playerUUID);

            if (bedOwner != null) {
                bedOwner.sendMessage(ChatColor.RED + "당신의 침대가 파괴되었습니다! 더 이상 리스폰할 수 없습니다.");

                // 침대 위치 정보 제거
                bedLocations.remove(bedLocation);

                // 플레이어가 사망한 경우 탈락 처리
                if (bedOwner.isDead()) {
                    eliminatePlayer(bedOwner);
                }
            }
        }
    }

    public void handlePlayerDeath(PlayerDeathEvent event) {
        // 아이템 드롭 방지
        event.getDrops().clear();

        // 보급품 아이템 제거
        Player player = event.getEntity();
        Inventory inv = player.getInventory();
        for (ItemStack item : supplyItems) {
            inv.remove(item);
        }

        // 플레이어의 침대가 존재하는지 확인
        if (!bedExistsForPlayer(player)) {
            // 플레이어 탈락 처리
            eliminatePlayer(player);
        }
    }

    public void handleBlockBreak(BlockBreakEvent event) {
        // 아이템 드롭 방지
        event.setDropItems(false);

        Block block = event.getBlock();

        // 블록이 침대인지 확인
        if (block.getType().toString().endsWith("_BED")) {
            Location bedLocation = block.getLocation();

            if (bedLocations.containsKey(bedLocation)) {
                UUID playerUUID = bedLocations.get(bedLocation);
                Player bedOwner = Bukkit.getPlayer(playerUUID);

                if (bedOwner != null) {
                    bedOwner.sendMessage(ChatColor.RED + "당신의 침대가 파괴되었습니다! 더 이상 리스폰할 수 없습니다.");

                    // 침대 위치 정보 제거
                    bedLocations.remove(bedLocation);

                    // 플레이어가 사망한 경우 탈락 처리
                    if (bedOwner.isDead()) {
                        eliminatePlayer(bedOwner);
                    }
                }
            }
        }
    }

    public void handleBlockPlace(BlockPlaceEvent event) {
        if (!gameStarted) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        // 배치된 블록이 침대인지 확인
        if (block.getType().toString().endsWith("_BED")) {
            // 플레이어의 리스폰 포인트 설정
            Location bedLocation = block.getLocation();
            player.setBedSpawnLocation(bedLocation, true);
            player.sendMessage(ChatColor.GREEN + "침대를 설치하고 리스폰 지점을 설정했습니다.");

            // 침대 위치와 플레이어 매핑
            bedLocations.put(bedLocation, player.getUniqueId());
        }
    }

    private boolean bedExistsForPlayer(Player player) {
        UUID playerUUID = player.getUniqueId();
        return bedLocations.containsValue(playerUUID);
    }

    private void eliminatePlayer(Player player) {
        player.sendMessage(ChatColor.RED + "침대가 파괴되어 게임에서 탈락했습니다.");
        player.setGameMode(GameMode.SPECTATOR);
        gamePlayers.remove(player.getUniqueId());

        // 남은 플레이어 수 확인
        if (gamePlayers.size() == 1) {
            UUID winnerUUID = gamePlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerUUID);
            if (winner != null) {
                Bukkit.broadcastMessage(ChatColor.GOLD + winner.getName() + "님이 승리했습니다!");
            }
            endGame();
        } else if (gamePlayers.isEmpty()) {
            // 남은 플레이어가 없는 경우 게임 종료
            Bukkit.broadcastMessage(ChatColor.YELLOW + "모든 플레이어가 탈락했습니다. 게임을 종료합니다.");
            endGame();
        }
    }

    public void handleItemSpawn(ItemSpawnEvent event) {
        // Prevent items from appearing when blocks are broken
        // Allow items if they are supply items
        ItemStack item = event.getEntity().getItemStack();
        if (!supplyItems.contains(item)) {
            event.setCancelled(true);
        }
    }

    public void handleItemPickup(EntityPickupItemEvent event) {
        // Allow players to pick up supply items
        ItemStack item = event.getItem().getItemStack();
        if (supplyItems.contains(item)) {
            return;
        }
        // Prevent picking up any other items
        event.setCancelled(true);
    }

    public void handlePlayerMove(PlayerMoveEvent event) {
        if (!gameStarted || !gamePlayers.contains(event.getPlayer().getUniqueId())) return;

        // 플레이어가 실제로 이동했는지 확인
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        if (!gameArea.isInside(event.getTo())) {
            if (!countdownTasks.containsKey(player.getUniqueId())) {
                player.sendTitle(ChatColor.RED + "경고!", "게임 영역을 벗어나고 있습니다!", 10, 70, 20);
                int taskId = startCountdown(player);
                countdownTasks.put(player.getUniqueId(), taskId);
            }
        } else {
            if (countdownTasks.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().cancelTask(countdownTasks.get(player.getUniqueId()));
                countdownTasks.remove(player.getUniqueId());
                player.sendTitle("", "", 0, 0, 0);
            }
        }

        if (gamePlayers.size() == 2) {
            for (UUID uuid : gamePlayers) {
                player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.setGlowing(true);
                }
            }
        } else {
            for (UUID uuid : gamePlayers) {
                player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.setGlowing(false);
                }
            }
        }
    }

    private int startCountdown(Player player) {
        return new BukkitRunnable() {
            int countdown = 5; // 5초 카운트다운

            @Override
            public void run() {
                if (countdown <= 0) {
                    player.setHealth(0);
                    countdownTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                player.sendTitle(ChatColor.RED + "" + countdown + "초 안에 돌아오세요!", "", 0, 20, 0);
                countdown--;
            }
        }.runTaskTimer(plugin, 0, 20).getTaskId();
    }

    public void handlePlayerQuit(PlayerQuitEvent event) {
        // 게임 중 플레이어가 나간 경우 처리
        UUID playerId = event.getPlayer().getUniqueId();
        gamePlayers.remove(playerId);
        if (countdownTasks.containsKey(playerId)) {
            Bukkit.getScheduler().cancelTask(countdownTasks.get(playerId));
            countdownTasks.remove(playerId);
        }
    }

    // In-game GUI for configuring supply items
    public void openSupplyConfigGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.WHITE + "보급품 설정");

        // Populate the GUI with current supply items
        for (int i = 0; i < supplyItems.size() && i < 27; i++) {
            gui.setItem(i, supplyItems.get(i));
        }

        player.openInventory(gui);
    }

    // Handle inventory clicks in the GUI
    public void handleInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GREEN + "보급품 설정")) {
            // 이벤트를 취소하지 않고 플레이어가 아이템을 이동할 수 있도록 허용
            Inventory clickedInventory = event.getClickedInventory();
            Inventory guiInventory = event.getView().getTopInventory();
            Player player = (Player) event.getWhoClicked();

            // 클릭한 인벤토리가 null인지 확인
            if (clickedInventory == null) {
                return;
            }

            // 이벤트 이후에 보급품 아이템 목록 업데이트
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateSupplyItemsFromGUI(guiInventory);
            }, 1L);
        }
    }

    private void updateSupplyItemsFromGUI(Inventory guiInventory) {
        supplyItems.clear();
        for (ItemStack item : guiInventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                supplyItems.add(item.clone());
            }
        }
    }

    public void giveStarterItems(Player player) {
        player.getInventory().clear();

        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.FLAME, 1, true);
            bow.setItemMeta(meta);
        }

        // 기본 아이템 지급 (침대 제외)
        player.getInventory().addItem(new ItemStack(Material.NETHERITE_SWORD));
        player.getInventory().addItem(bow);
        player.getInventory().addItem(new ItemStack(Material.ARROW, 32));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 8));
        player.getInventory().addItem(new ItemStack(Material.WHITE_WOOL, 32));
    }

    /**
     * 맵 상태를 백업하는 메서드
     * 게임 시작 시 선택된 영역의 모든 블록 상태를 저장합니다.
     */
    private void saveGameAreaState() {
        originalMapState.clear();
        if (gameArea == null) return;
        int minX = Math.min(gameArea.getPos1().getBlockX(), gameArea.getPos2().getBlockX());
        int minY = Math.min(gameArea.getPos1().getBlockY(), gameArea.getPos2().getBlockY());
        int minZ = Math.min(gameArea.getPos1().getBlockZ(), gameArea.getPos2().getBlockZ());

        int maxX = Math.max(gameArea.getPos1().getBlockX(), gameArea.getPos2().getBlockX());
        int maxY = Math.max(gameArea.getPos1().getBlockY(), gameArea.getPos2().getBlockY());
        int maxZ = Math.max(gameArea.getPos1().getBlockZ(), gameArea.getPos2().getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = gameWorld.getBlockAt(x, y, z);
                    BlockState state = block.getState();
                    originalMapState.add(state);
                }
            }
        }
    }

    /**
     * 맵 상태를 복원하는 메서드
     * /맵초기화 명령어 실행 시 이전에 백업한 상태로 맵을 되돌립니다.
     */
    public void restoreGameAreaState() {
        if (originalMapState.isEmpty()) {
            return;
        }
        for (BlockState state : originalMapState) {
            Block block = state.getBlock();
            block.setType(state.getType());
            block.setBlockData(state.getBlockData());
        }
        Bukkit.broadcastMessage(ChatColor.GREEN + "맵이 초기화되었습니다.");
    }

    private void startBombardment() {
        if (!gameStarted) return;
        Bukkit.broadcastMessage(ChatColor.RED + " ");
        Bukkit.broadcastMessage(ChatColor.RED + "하늘에서 랜덤 폭격이 시작되었습니다! 조심하세요!");
        Bukkit.broadcastMessage(ChatColor.RED + " ");

        // 30초(600틱) 주기로 폭격을 가하며, tntCount 2배씩 증가
        bombardmentTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameStarted) {
                    // 게임이 끝나면 폭격 중단
                    this.cancel();
                    return;
                }

                // TNT 떨어뜨리기
                int dropCount = Math.min(tntCount, MAX_TNT); // MAX_TNT(1000) 이하로 제한
                for (int i = 0; i < dropCount; i++) {
                    spawnRandomTNT();
                }

                // 다음 라운드에 TNT 수 2배 증가(최대 1000 이상은 안 가지만 로직상 문제없음)
                tntCount *= 2;
                if (tntCount > MAX_TNT) {
                    tntCount = MAX_TNT; // 안전장치
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 30).getTaskId(); // 30초마다 실행
    }

    /**
     * 게임 영역 내 랜덤 좌표를 선택하고 상공에서 TNT를 소환합니다.
     */
    private void spawnRandomTNT() {
        int minX = Math.min(gameArea.getPos1().getBlockX(), gameArea.getPos2().getBlockX());
        int minY = Math.min(gameArea.getPos1().getBlockY(), gameArea.getPos2().getBlockY());
        int minZ = Math.min(gameArea.getPos1().getBlockZ(), gameArea.getPos2().getBlockZ());

        int maxX = Math.max(gameArea.getPos1().getBlockX(), gameArea.getPos2().getBlockX());
        int maxY = Math.max(gameArea.getPos1().getBlockY(), gameArea.getPos2().getBlockY());
        int maxZ = Math.max(gameArea.getPos1().getBlockZ(), gameArea.getPos2().getBlockZ());

        Random rand = new Random();
        int x = rand.nextInt((maxX - minX) + 1) + minX;
        int z = rand.nextInt((maxZ - minZ) + 1) + minZ;

        // 하늘 위 특정 높이 (예: Y=200)에서 TNT 떨어뜨리기
        int spawnY = 110;
        Location tntLocation = new Location(gameWorld, x + 0.5, spawnY, z + 0.5);
        TNTPrimed tnt = gameWorld.spawn(tntLocation, TNTPrimed.class);
        tnt.setFuseTicks(100); // 적당한 퓨즈 시간 설정
    }

    public void handleEntityExplode(EntityExplodeEvent event) {
        // 폭발로 파괴된 블록들 중 침대가 있는지 확인
        List<Block> destroyedBlocks = event.blockList();
        for (Block block : destroyedBlocks) {
            if (block.getType().toString().endsWith("_BED")) {
                handleBedDestruction(block);
            }
        }
    }
}


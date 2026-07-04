package xuanmo.aubade.core.island;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import xuanmo.arcartxsuite.api.aubade.addon.GameModeAddon;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.arcartxsuite.api.aubade.island.IslandManager;
import xuanmo.arcartxsuite.api.aubade.island.IslandMember;
import xuanmo.arcartxsuite.api.aubade.island.Role;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.blueprint.BlueprintPaster;
import xuanmo.aubade.core.blueprint.BlueprintParser;
import xuanmo.aubade.core.storage.JdbcIslandRepository;

/**
 * 岛屿管理器实现。
 */
public class IslandManagerImpl implements IslandManager {

  private final AubadeCore plugin;
  private final Logger logger;
  private final IslandCache cache;
  private final IslandGrid grid;
  private final IslandFactory factory;
  private final JdbcIslandRepository repository;
  private final BlueprintPaster paster;

  private final Map<UUID, UUID> pendingInvites = new HashMap<>(); // 被邀请玩家 -> 岛屿ID

  public IslandManagerImpl(AubadeCore plugin, IslandGrid grid, JdbcIslandRepository repository) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.cache = new IslandCache();
    this.grid = grid;
    this.factory = new IslandFactory(grid);
    this.repository = repository;
    this.paster = new BlueprintPaster(plugin);
  }

  @Override
  public Island createIsland(Player player, String gameModeId) {
    return createIsland(player, gameModeId, "default");
  }

  public Island createIsland(Player player, String gameModeId, String blueprintId) {
    UUID owner = player.getUniqueId();
    if (cache.getByOwner(owner).isPresent()) {
      player.sendMessage("§c你已经拥有一个岛屿了。");
      return null;
    }

    var addonManager = plugin.getLifecycleManager().getAddonLifecycleManager();
    GameModeAddon gameMode = addonManager != null ? addonManager.getGameMode(gameModeId) : null;
    if (gameMode == null) {
      player.sendMessage("\u00a7c当前默认游戏模式未注册，请联系管理员。");
      return null;
    }
    if (gameMode == null) {
      player.sendMessage("Â§c当前默认游戏模式未注册，请联系管理员。");
      return null;
    }

    // 获取或创建游戏世界
    org.bukkit.World world = plugin.getServer().getWorld("aubade_skyblock");
    if (world == null) {
      world = plugin.getServer().getWorlds().get(0);
    }

    Island island = factory.create(owner, world, gameMode);
    island.getMembers().put(owner, new IslandMember(owner, Role.OWNER));

    // 通过蓝图生成器获取并粘贴蓝图
    xuanmo.aubade.core.blueprint.Blueprint blueprint = resolveBlueprint(blueprintId);
    paster.pasteSync(blueprint, island.getCenter());

    Location safeHomeBase = findSafeHomeBase(island.getCenter());
    if (safeHomeBase != null) {
      island.setCenter(safeHomeBase);
    }

    cache.put(island);
    repository.save(island);

    var skyPlayer = plugin.getPlayerManager().getPlayer(player);
    skyPlayer.setIslandId(island.getUniqueId());
    plugin.getPlayerManager().savePlayer(skyPlayer);

    logger.info("[岛屿] 玩家 " + player.getName() + " 使用蓝图 [" + blueprint.getId() + "] 创建了新岛屿: " + island.getUniqueId());
    return island;
  }

  private Location findSafeHomeBase(Location origin) {
    if (origin == null || origin.getWorld() == null) {
      return origin;
    }
    org.bukkit.World world = origin.getWorld();
    int x = origin.getBlockX();
    int z = origin.getBlockZ();
    int startY = Math.max(world.getMinHeight(), origin.getBlockY());
    int maxY = world.getMaxHeight() - 2;
    for (int y = startY; y <= maxY; y++) {
      if (world.getBlockAt(x, y, z).getType().isSolid()
          && world.getBlockAt(x, y + 1, z).isEmpty()
          && world.getBlockAt(x, y + 2, z).isEmpty()) {
        return new Location(world, x, y, z);
      }
    }
    return origin;
  }

  private xuanmo.aubade.core.blueprint.Blueprint resolveBlueprint(String blueprintId) {
    var addon = plugin.getLifecycleManager().getAddonLifecycleManager().getExtension("blueprint_generator");
    if (addon instanceof xuanmo.aubade.core.features.blueprint.BlueprintGeneratorAddon gen) {
      var registry = gen.getRegistry();
      if (registry != null && registry.hasBlueprint(blueprintId)) {
        return registry.getBlueprint(blueprintId);
      }
    }
    return BlueprintParser.createDefaultBlueprint();
  }

  @Override
  public boolean deleteIsland(Island island) {
    cache.remove(island.getUniqueId());
    repository.delete(island);
    logger.info("[岛屿] 岛屿已删除: " + island.getUniqueId());
    return true;
  }

  @Override
  public Optional<Island> getIslandById(UUID uniqueId) {
    Optional<Island> cached = cache.getById(uniqueId);
    if (cached.isPresent()) {
      return cached;
    }
    Optional<Island> fromDb = repository.findById(uniqueId);
    fromDb.ifPresent(cache::put);
    return fromDb;
  }

  @Override
  public Optional<Island> getIslandByOwner(UUID owner) {
    Optional<Island> cached = cache.getByOwner(owner);
    if (cached.isPresent()) {
      return cached;
    }
    Optional<Island> fromDb = repository.findByOwner(owner);
    fromDb.ifPresent(cache::put);
    return fromDb;
  }

  @Override
  public Optional<Island> getIslandAt(Location location) {
    // TODO: 优化为空间索引查询
    for (Island island : getCachedIslands()) {
      if (island.inProtectionRange(location)) {
        return Optional.of(island);
      }
    }
    return Optional.empty();
  }

  @Override
  public int getIslandCount() {
    return cache.size();
  }

  @Override
  public List<Island> getIslandsInWorld(String worldName) {
    return getCachedIslands().stream()
        .filter(i -> i.getWorld() != null && i.getWorld().getName().equals(worldName))
        .toList();
  }

  @Override
  public void saveIsland(Island island) {
    repository.save(island);
  }

  @Override
  public boolean invitePlayer(Island island, Player player) {
    pendingInvites.put(player.getUniqueId(), island.getUniqueId());
    String ownerName = plugin.getServer().getOfflinePlayer(island.getOwner()).getName();
    if (ownerName == null) {
      ownerName = "岛主";
    }
    player.sendMessage("§e" + ownerName + " §a邀请你加入岛屿，使用 §e/island accept §a接受。");
    return true;
  }

  @Override
  public boolean acceptInvite(Player player) {
    UUID islandId = pendingInvites.remove(player.getUniqueId());
    if (islandId == null) {
      player.sendMessage("§c你没有待处理的邀请。");
      return false;
    }
    Optional<Island> opt = getIslandById(islandId);
    if (opt.isEmpty()) {
      player.sendMessage("§c邀请已失效（岛屿不存在）。");
      return false;
    }
    Island island = opt.get();
    island.getMembers().put(player.getUniqueId(), new IslandMember(player.getUniqueId(), Role.MEMBER));
    saveIsland(island);
    player.sendMessage("§a你已加入岛屿！");
    return true;
  }

  @Override
  public boolean kickMember(Island island, UUID player) {
    if (island.getOwner().equals(player)) {
      return false; // 不能踢岛主
    }
    island.getMembers().remove(player);
    saveIsland(island);
    return true;
  }

  public IslandCache getCache() {
    return cache;
  }

  public IslandGrid getGrid() {
    return grid;
  }

  public List<Island> getCachedIslands() {
    return List.copyOf(cache.values());
  }
}


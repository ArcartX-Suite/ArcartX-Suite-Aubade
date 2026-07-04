package xuanmo.aubade.core.features.visit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import xuanmo.arcartxsuite.api.aubade.addon.AddonDescriptor;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.command.IslandVisitCommand;
import xuanmo.aubade.core.features.AbstractExtensionAddon;

/**
 * 岛屿参观扩展组件。
 * 允许玩家安全地参观其他岛屿（访客模式，无建造权限）。
 */
public class VisitAddon extends AbstractExtensionAddon {

  // 记录当前处于访客模式的玩家 -> 目标岛屿
  private final Map<UUID, UUID> visitingMap = new HashMap<>();
  private final Map<UUID, Location> returnLocations = new HashMap<>();

  public VisitAddon(AubadeCore core) {
    super(core, AddonDescriptor.builder("visit")
        .name("岛屿参观")
        .version("1.0.0")
        .mainClass(VisitAddon.class.getName())
        .build());
  }

  @Override
  public String getExtensionId() {
    return "visit";
  }

  @Override
  public String getFriendlyName() {
    return "岛屿参观";
  }

  @Override
  public void onEnable() {
    super.onEnable();
    try {
      getCommandManager().registerSubCommand("island", new IslandVisitCommand(core));
    } catch (Exception e) {
      core.getLogger().warning("[Visit] 注册命令失败: " + e.getMessage());
    }
    core.getLogger().info("[Visit] 岛屿参观扩展已启用。");
  }

  @Override
  public void onDisable() {
    super.onDisable();
    visitingMap.clear();
    returnLocations.clear();
    core.getLogger().info("[Visit] 岛屿参观扩展已禁用。");
  }

  /**
   * 参观指定玩家的岛屿。
   */
  public boolean visit(Player visitor, UUID targetOwnerId) {
    if (isVisiting(visitor.getUniqueId())) {
      visitor.sendMessage("§c你正在参观其他岛屿，请先使用 §e/island leave §c退出参观。");
      return false;
    }
    if (visitor.getUniqueId().equals(targetOwnerId)) {
      visitor.sendMessage("§c你已经在自己的岛屿上了。");
      return false;
    }

    Optional<Island> opt = getIslandManager().getIslandByOwner(targetOwnerId);
    if (opt.isEmpty()) {
      visitor.sendMessage("§c目标玩家没有岛屿。");
      return false;
    }
    Island island = opt.get();
    if (island.isLocked()) {
      visitor.sendMessage("§c该岛屿已锁定，无法参观。");
      return false;
    }
    if (island.getBannedPlayers().contains(visitor.getUniqueId())) {
      visitor.sendMessage("§c你被禁止进入该岛屿。");
      return false;
    }

    Location center = island.getCenter();
    if (center == null || center.getWorld() == null) {
      visitor.sendMessage("§c目标岛屿世界未加载。");
      return false;
    }

    returnLocations.put(visitor.getUniqueId(), visitor.getLocation().clone());
    visitor.teleport(center.clone().add(0, 2, 0));
    visitingMap.put(visitor.getUniqueId(), island.getUniqueId());

    String islandName = island.getName() != null ? island.getName() : "未命名岛屿";
    visitor.sendMessage("§a正在参观 " + islandName + " §a的岛屿...");
    return true;
  }

  /**
   * 离开参观模式，返回原位置或主世界出生点。
   */
  public boolean leaveVisit(Player visitor) {
    UUID islandId = visitingMap.remove(visitor.getUniqueId());
    if (islandId == null) {
      return false;
    }

    Location returnLocation = returnLocations.remove(visitor.getUniqueId());
    if (returnLocation != null && returnLocation.getWorld() != null) {
      visitor.teleport(returnLocation);
      visitor.sendMessage("§a已返回参观前的位置。");
      return true;
    }

    Location spawn = getFallbackSpawn(visitor);
    if (spawn != null) {
      visitor.teleport(spawn);
    }
    visitor.sendMessage("§a已退出参观模式。");
    return true;
  }

  /**
   * 检查玩家是否处于参观模式。
   */
  public boolean isVisiting(UUID playerId) {
    return visitingMap.containsKey(playerId);
  }

  /**
   * 获取玩家正在参观的岛屿 ID。
   */
  public Optional<UUID> getVisitingIsland(UUID playerId) {
    return Optional.ofNullable(visitingMap.get(playerId));
  }

  public Optional<Location> getReturnLocation(UUID playerId) {
    return Optional.ofNullable(returnLocations.get(playerId));
  }

  private Location getFallbackSpawn(Player visitor) {
    World world = visitor.getWorld();
    if (world != null) {
      return world.getSpawnLocation();
    }
    if (!Bukkit.getWorlds().isEmpty()) {
      return Bukkit.getWorlds().get(0).getSpawnLocation();
    }
    return null;
  }
}

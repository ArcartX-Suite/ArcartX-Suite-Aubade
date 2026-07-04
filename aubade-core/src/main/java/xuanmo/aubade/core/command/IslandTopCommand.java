package xuanmo.aubade.core.command;

import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.arcartxsuite.api.aubade.permission.Permission;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.features.level.LevelAddon;
import xuanmo.aubade.core.features.level.LevelTopCache;

/**
 * /island top [页码] — 查看岛屿等级排行榜。
 */
public class IslandTopCommand extends CompositeCommand {

  private final AubadeCore core;
  private static final int PER_PAGE = 10;

  public IslandTopCommand(AubadeCore core) {
    super("top", "查看岛屿等级排行榜", Permission.PLAYER_TOP, true);
    this.core = core;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (!checkPlayer(sender)) {
      sender.sendMessage("§c此命令只能由玩家执行。");
      return true;
    }

    Player player = (Player) sender;
    if (args.length == 0 && core.getUiManager() != null && core.getUiManager().openUi(player, "aubade_top")) {
      return true;
    }

    LevelAddon levelAddon = resolveLevelAddon();
    if (levelAddon == null) {
      sender.sendMessage("§c等级功能组件未启用。");
      return true;
    }

    int page = 1;
    if (args.length > 0) {
      try {
        page = Integer.parseInt(args[0]);
        if (page < 1) page = 1;
      } catch (NumberFormatException e) {
        player.sendMessage("§c无效的页码。");
        return true;
      }
    }

    // 获取 Top 数据
    List<LevelTopCache.TopEntry> topList = levelAddon.getTop(PER_PAGE * page);
    if (topList.isEmpty()) {
      player.sendMessage("§c暂无排行榜数据。");
      return true;
    }

    int totalPages = (topList.size() + PER_PAGE - 1) / PER_PAGE;
    if (page > totalPages) page = totalPages;

    int start = (page - 1) * PER_PAGE;
    int end = Math.min(start + PER_PAGE, topList.size());

    player.sendMessage("§6========== 岛屿等级排行榜 (第 " + page + "/" + totalPages + " 页) ==========");
    for (int i = start; i < end; i++) {
      LevelTopCache.TopEntry entry = topList.get(i);
      Optional<Island> opt = core.getLifecycleManager().getIslandManager().getIslandById(entry.islandId());
      String islandName = "未知岛屿";
      String ownerName = "未知";
      if (opt.isPresent()) {
        Island island = opt.get();
        islandName = island.getName() != null ? island.getName() : "未命名";
        org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
        ownerName = owner.getName() != null ? owner.getName() : "未知";
      }
      String rankColor = (i == 0) ? "§6" : (i == 1) ? "§7" : (i == 2) ? "§c" : "§f";
      player.sendMessage(rankColor + "#" + (i + 1) + " §e" + islandName + " §7(岛主: " + ownerName + ") §a等级: " + entry.level());
    }

    // 显示玩家自身排名
    Optional<Island> myIsland = core.getLifecycleManager().getIslandManager().getIslandByOwner(player.getUniqueId());
    if (myIsland.isPresent()) {
      int myRank = levelAddon.getTopCache().getRank(myIsland.get().getUniqueId());
      if (myRank > 0) {
        player.sendMessage("§b你的排名: §e#" + myRank + " §7(等级: " + myIsland.get().getLevel() + ")");
      }
    }
    player.sendMessage("§6==================================================");
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 0) {
      return List.of("1", "2", "3");
    }
    return List.of();
  }

  private LevelAddon resolveLevelAddon() {
    var addon = core.getLifecycleManager().getAddonLifecycleManager().getAddon("level");
    if (addon instanceof LevelAddon level) {
      return level;
    }
    return null;
  }
}

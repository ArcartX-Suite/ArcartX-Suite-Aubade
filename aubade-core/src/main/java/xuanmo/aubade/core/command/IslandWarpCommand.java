package xuanmo.aubade.core.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.arcartxsuite.api.aubade.permission.Permission;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.warp.IslandWarpHelper;

/**
 * /island warp [名称] — 查看传送点列表或传送到指定传送点。
 */
public class IslandWarpCommand extends CompositeCommand {

  private final AubadeCore core;

  public IslandWarpCommand(AubadeCore core) {
    super("warp", "岛屿传送点", Permission.PLAYER_WARP, true);
    this.core = core;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (!checkPlayer(sender)) {
      sender.sendMessage("§c此命令只能由玩家执行。");
      return true;
    }
    Player player = (Player) sender;
    var manager = core.getLifecycleManager().getIslandManager();
    Optional<Island> opt = manager.getIslandByOwner(player.getUniqueId());
    if (opt.isEmpty()) {
      player.sendMessage("§c你还没有岛屿。");
      return true;
    }
    Island island = opt.get();
    Map<String, Location> warps = IslandWarpHelper.getWarps(island);

    if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
      if (core.getUiManager() != null && core.getUiManager().openUi(player, "warp_board")) {
        return true;
      }
      // 列出传送点
      if (warps.isEmpty()) {
        player.sendMessage("§c当前岛屿没有设置传送点。使用 §e/island setwarp <名称> §c创建。");
        return true;
      }
      player.sendMessage("§6========== 岛屿传送点 ==========");
      for (Map.Entry<String, Location> entry : warps.entrySet()) {
        Location loc = entry.getValue();
        player.sendMessage("§e" + entry.getKey() + " §7- §f" + loc.getWorld().getName()
            + " (" + (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ() + ")");
      }
      player.sendMessage("§6================================");
      return true;
    }

    // 传送到指定传送点
    String warpName = args[0];
    Location loc = warps.get(warpName);
    if (loc == null) {
      player.sendMessage("§c传送点 §e" + warpName + " §c不存在。");
      return true;
    }
    player.teleport(loc);
    player.sendMessage("§a已传送到 §e" + warpName + "§a。");
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) return List.of();
    if (args.length == 0) {
      var manager = core.getLifecycleManager().getIslandManager();
      var opt = manager.getIslandByOwner(((Player) sender).getUniqueId());
      if (opt.isEmpty()) return List.of();
      return new ArrayList<>(IslandWarpHelper.getWarps(opt.get()).keySet());
    }
    return List.of();
  }
}


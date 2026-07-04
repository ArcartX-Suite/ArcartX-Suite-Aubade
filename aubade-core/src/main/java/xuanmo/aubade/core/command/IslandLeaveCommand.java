package xuanmo.aubade.core.command;

import java.util.List;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.arcartxsuite.api.aubade.permission.Permission;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.features.visit.VisitAddon;

/**
 * /island leave — 离开当前所属岛屿。
 */
public class IslandLeaveCommand extends CompositeCommand {

  private final AubadeCore core;

  public IslandLeaveCommand(AubadeCore core) {
    super("leave", "离开当前岛屿", Permission.PLAYER_LEAVE, true);
    this.core = core;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (!checkPlayer(sender)) {
      sender.sendMessage("§c此命令只能由玩家执行。");
      return true;
    }
    Player player = (Player) sender;

    VisitAddon visitAddon = getVisitAddon();
    if (visitAddon != null && visitAddon.isVisiting(player.getUniqueId())) {
      visitAddon.leaveVisit(player);
      return true;
    }

    var manager = core.getLifecycleManager().getIslandManager();
    Optional<Island> opt = manager.getIslandByOwner(player.getUniqueId());

    // 先查是不是岛主
    if (opt.isPresent()) {
      player.sendMessage("§c你是岛主，无法直接离开。请使用 §e/island delete confirm §c删除岛屿，或转让所有权。");
      return true;
    }

    // 再查是不是成员
    for (Island island : ((xuanmo.aubade.core.island.IslandManagerImpl) manager).getCachedIslands()) {
      if (island.getMembers().containsKey(player.getUniqueId())) {
        island.getMembers().remove(player.getUniqueId());
        manager.saveIsland(island);
        player.sendMessage("§a你已离开岛屿。");
        return true;
      }
    }

    player.sendMessage("§c你当前不属于任何岛屿。");
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    return List.of();
  }

  private VisitAddon getVisitAddon() {
    var addonLifecycleManager = core.getLifecycleManager().getAddonLifecycleManager();
    if (addonLifecycleManager == null) {
      return null;
    }
    var addon = addonLifecycleManager.getExtension("visit");
    return addon instanceof VisitAddon visitAddon ? visitAddon : null;
  }
}

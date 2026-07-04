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

/**
 * /island info [玩家] — 查看岛屿信息。
 */
public class IslandInfoCommand extends CompositeCommand {

  private final AubadeCore core;

  public IslandInfoCommand(AubadeCore core) {
    super("info", "查看岛屿信息", Permission.PLAYER_INFO, true);
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

    Optional<Island> opt;
    if (args.length > 0) {
      Player target = Bukkit.getPlayer(args[0]);
      if (target == null) {
        player.sendMessage("§c玩家不在线: " + args[0]);
        return true;
      }
      opt = manager.getIslandByOwner(target.getUniqueId());
    } else {
      if (core.getUiManager() != null && core.getUiManager().openUi(player, "member_manage")) {
        return true;
      }
      opt = manager.getIslandByOwner(player.getUniqueId());
    }

    if (opt.isEmpty()) {
      player.sendMessage("§c目标玩家没有岛屿。");
      return true;
    }

    Island island = opt.get();
    player.sendMessage("§6========== 岛屿信息 ==========");
    player.sendMessage("§e岛屿ID: §f" + island.getUniqueId().toString().substring(0, 8));
    player.sendMessage("§e岛主: §f" + Bukkit.getOfflinePlayer(island.getOwner()).getName());
    player.sendMessage("§e成员数: §f" + island.getMembers().size());
    player.sendMessage("§e保护范围: §f" + island.getProtectionRange() + " 格");
    player.sendMessage("§e等级: §f" + island.getLevel());
    player.sendMessage("§e银行余额: §f" + island.getBankBalance());
    player.sendMessage("§e锁岛: §f" + (island.isLocked() ? "是" : "否"));
    player.sendMessage("§6==============================");
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    return List.of();
  }
}


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
 * /island invite <player> — 邀请玩家加入岛屿。
 */
public class IslandInviteCommand extends CompositeCommand {

  private final AubadeCore core;

  public IslandInviteCommand(AubadeCore core) {
    super("invite", "邀请玩家加入岛屿", Permission.PLAYER, true);
    this.core = core;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (!checkPlayer(sender)) {
      sender.sendMessage("§c此命令只能由玩家执行。");
      return true;
    }
    Player player = (Player) sender;
    if (args.length == 0 && core.getUiManager() != null && core.getUiManager().openUi(player, "aubade_invite")) {
      return true;
    }
    if (args.length == 0) {
      sender.sendMessage("§c用法: /island invite <玩家>");
      return true;
    }
    Player target = Bukkit.getPlayer(args[0]);
    if (target == null) {
      player.sendMessage("§c玩家不在线。");
      return true;
    }
    var manager = core.getLifecycleManager().getIslandManager();
    Optional<Island> opt = manager.getIslandByOwner(player.getUniqueId());
    if (opt.isEmpty()) {
      player.sendMessage("§c你还没有岛屿。");
      return true;
    }
    manager.invitePlayer(opt.get(), target);
    player.sendMessage("§a已向 §e" + target.getName() + " §a发送岛屿邀请。");
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
          .toList();
    }
    return List.of();
  }
}

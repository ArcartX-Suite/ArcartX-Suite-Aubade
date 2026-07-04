package xuanmo.aubade.core.command;

import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.arcartxsuite.api.aubade.permission.Permission;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.features.visit.VisitAddon;

/**
 * /island visit <玩家> — 参观指定玩家的岛屿。
 */
public class IslandVisitCommand extends CompositeCommand {

  private final AubadeCore core;

  public IslandVisitCommand(AubadeCore core) {
    super("visit", "参观其他岛屿", Permission.PLAYER, true);
    this.core = core;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (!checkPlayer(sender)) {
      sender.sendMessage("§c此命令只能由玩家执行。");
      return true;
    }
    if (args.length == 0) {
      sender.sendMessage("§c用法: /island visit <玩家>");
      return true;
    }

    Player player = (Player) sender;
    VisitAddon addon = getVisitAddon();
    if (addon == null) {
      player.sendMessage("§c参观功能暂不可用。");
      return true;
    }

    String targetName = args[0];
    OfflinePlayer target = resolveTarget(targetName);
    if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
      player.sendMessage("§c目标玩家不存在: §e" + targetName);
      return true;
    }

    if (player.getUniqueId().equals(target.getUniqueId())) {
      player.sendMessage("§c你已经在自己的岛屿上了。");
      return true;
    }

    addon.visit(player, target.getUniqueId());
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length > 1) {
      return List.of();
    }
    String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
    return Bukkit.getOnlinePlayers().stream()
        .map(Player::getName)
        .filter(name -> prefix.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(prefix))
        .sorted()
        .toList();
  }

  private OfflinePlayer resolveTarget(String name) {
    Player online = Bukkit.getPlayerExact(name);
    if (online != null) {
      return online;
    }
    OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
    if (offline != null && (offline.isOnline() || offline.hasPlayedBefore())) {
      return offline;
    }
    return null;
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

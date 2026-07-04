package xuanmo.aubade.core.command;

import java.util.List;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.arcartxsuite.api.aubade.island.IslandPermission;
import xuanmo.arcartxsuite.api.aubade.permission.Permission;
import xuanmo.aubade.core.AubadeCore;

/**
 * /island settings [flag] [true/false] — 查看/修改岛屿设置。
 */
public class IslandSettingsCommand extends CompositeCommand {

  private final AubadeCore core;

  public IslandSettingsCommand(AubadeCore core) {
    super("settings", "岛屿设置", Permission.PLAYER_SETTINGS, true);
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

    if (args.length == 0) {
      if (core.getUiManager() != null && core.getUiManager().openUi(player, "team_settings")) {
        return true;
      }
      player.sendMessage("§6========== 岛屿设置 ==========");
      for (IslandPermission perm : IslandPermission.values()) {
        boolean val = island.getFlags().getOrDefault(perm.name(), perm.getDefaultValue());
        player.sendMessage("§e" + perm.getDisplayName() + ": §f" + (val ? "开启" : "关闭"));
      }
      player.sendMessage("§e锁岛: §f" + (island.isLocked() ? "是" : "否"));
      player.sendMessage("§6==============================");
      return true;
    }

    if (args.length < 2) {
      player.sendMessage("§c用法: /island settings <flag> <true/false>");
      return true;
    }

    String flagName = args[0].toUpperCase();
    IslandPermission perm;
    try {
      perm = IslandPermission.valueOf(flagName);
    } catch (IllegalArgumentException e) {
      player.sendMessage("§c未知设置项: " + args[0]);
      return true;
    }

    boolean value = Boolean.parseBoolean(args[1].toLowerCase());
    island.getFlags().put(perm.name(), value);
    manager.saveIsland(island);
    player.sendMessage("§a已设置 §e" + perm.getDisplayName() + " §a为 §f" + (value ? "开启" : "关闭") + "§a。");
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      List<String> list = new java.util.ArrayList<>();
      for (IslandPermission perm : IslandPermission.values()) {
        list.add(perm.name().toLowerCase());
      }
      list.add("locked");
      return list;
    }
    if (args.length == 2) {
      return List.of("true", "false");
    }
    return List.of();
  }
}


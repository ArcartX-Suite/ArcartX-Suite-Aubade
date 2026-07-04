package xuanmo.aubade.core.features.level.command;

import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.aubade.core.features.level.LevelAddon;

/**
 * /island level 子命令。
 * 计算并显示当前岛屿等级。
 */
public class LevelCommand extends CompositeCommand {

  private final LevelAddon addon;

  public LevelCommand(LevelAddon addon) {
    super("level", "计算并显示岛屿等级", "aubade.player.level", true);
    this.addon = addon;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (!checkPlayer(sender) || !checkPermission(sender)) {
      return false;
    }
    Player player = (Player) sender;
    if (args.length == 0 && core.getUiManager() != null && core.getUiManager().openUi(player, "island_level_display")) {
      return true;
    }
    Optional<Island> opt = addon.getIslandManager().getIslandByOwner(player.getUniqueId());
    if (opt.isEmpty()) {
      player.sendMessage("§c你还没有岛屿。");
      return false;
    }
    Island island = opt.get();
    player.sendMessage("§a正在计算岛屿等级...");

    addon.updateLevelAsync(island).thenAccept(level -> {
      player.sendMessage("§a你的岛屿等级为: §e" + level);
    }).exceptionally(ex -> {
      player.sendMessage("§c计算等级时出错: " + ex.getMessage());
      return null;
    });
    return true;
  }

  @Override
  public java.util.List<String> tabComplete(CommandSender sender, String[] args) {
    return java.util.Collections.emptyList();
  }
}


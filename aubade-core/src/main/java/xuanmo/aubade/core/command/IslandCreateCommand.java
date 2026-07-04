package xuanmo.aubade.core.command;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xuanmo.aubade.core.blueprint.BlueprintRegistry;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.arcartxsuite.api.aubade.permission.Permission;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.island.IslandManagerImpl;

/**
 * /island create [blueprint] — 创建新岛屿。
 */
public class IslandCreateCommand extends CompositeCommand {

  private final AubadeCore core;

  public IslandCreateCommand(AubadeCore core) {
    super("create", "创建新岛屿", Permission.PLAYER_CREATE, true);
    this.core = core;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (!checkPlayer(sender)) {
      sender.sendMessage("§c此命令只能由玩家执行。");
      return true;
    }
    Player player = (Player) sender;
    if (args.length == 0 && core.getUiManager() != null && core.getUiManager().openUi(player, "aubade_create")) {
      return true;
    }
    IslandManagerImpl manager = getIslandManager();
    if (manager == null) {
      player.sendMessage("§c岛屿管理器尚未初始化。");
      return true;
    }

    String blueprintId = args.length > 0 ? args[0] : "default";
    BlueprintRegistry registry = getBlueprintRegistry();
    if (registry != null) {
      if (!registry.hasBlueprint(blueprintId)) {
        player.sendMessage("§c未找到蓝图 §e" + blueprintId + "§c。");
        List<String> available = registry.getBlueprintIds();
        if (!available.isEmpty()) {
          player.sendMessage("§7可用蓝图: §f" + String.join("§7, §f", available));
        }
        return true;
      }
    } else {
      player.sendMessage("§c蓝图系统尚未初始化。");
      return true;
    }

    var island = manager.createIsland(player, core.getLifecycleManager().getCoreConfig().getDefaultGameMode(), blueprintId);
    if (island != null) {
      player.teleport(island.getCenter().clone().add(0, 1, 0));
      player.sendMessage("§a岛屿创建成功！使用 §e/island home §a返回。");
    }
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      BlueprintRegistry registry = getBlueprintRegistry();
      if (registry != null) {
        return registry.getBlueprintIds();
      }
      return List.of("default");
    }
    return List.of();
  }

  private IslandManagerImpl getIslandManager() {
    return core.getLifecycleManager().getIslandManager();
  }

  private BlueprintRegistry getBlueprintRegistry() {
    var addon = core.getLifecycleManager().getAddonLifecycleManager().getExtension("blueprint_generator");
    if (addon instanceof xuanmo.aubade.core.features.blueprint.BlueprintGeneratorAddon gen) {
      return gen.getRegistry();
    }
    return null;
  }
}

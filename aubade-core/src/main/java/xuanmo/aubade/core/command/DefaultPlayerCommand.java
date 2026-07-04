package xuanmo.aubade.core.command;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.arcartxsuite.api.aubade.permission.Permission;

/**
 * /island 根命令的默认 help 处理器。
 */
public class DefaultPlayerCommand extends CompositeCommand {

  private final AubadeCore core;

  public DefaultPlayerCommand(AubadeCore core) {
    super("help", "显示岛屿命令帮助", Permission.PLAYER, false);
    this.core = core;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (sender instanceof Player player && core.getUiManager() != null && core.getUiManager().openUi(player, "aubade_main")) {
      return true;
    }
    sender.sendMessage("§6========== §eAubade 岛屿帮助 §6==========");
    sender.sendMessage("§e/island create [蓝图] §7— 创建新岛屿");
    sender.sendMessage("§e/island delete §7— 删除当前岛屿");
    sender.sendMessage("§e/island home §7— 传送到岛屿出生点");
    sender.sendMessage("§e/island sethome §7— 设置岛屿出生点");
    sender.sendMessage("§e/island invite <玩家> §7— 邀请玩家加入");
    sender.sendMessage("§e/island accept §7— 接受邀请");
    sender.sendMessage("§e/island kick <玩家> §7— 踢出成员");
    sender.sendMessage("§e/island leave §7— 离开当前岛屿");
    sender.sendMessage("§e/island info §7— 查看岛屿信息");
    sender.sendMessage("§e/island top §7— 查看岛屿排行榜");
    sender.sendMessage("§6=======================================");
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    return List.of();
  }
}

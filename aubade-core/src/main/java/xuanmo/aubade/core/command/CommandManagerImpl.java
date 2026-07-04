package xuanmo.aubade.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import xuanmo.arcartxsuite.api.aubade.command.CommandManager;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.aubade.core.AubadeCore;

/**
 * 命令管理器实现。
 * 负责在模块内分发岛屿与管理子命令。
 */
public class CommandManagerImpl implements CommandManager {

  private final AubadeCore core;
  private final Logger logger;
  private final Map<String, CompositeCommand> playerSubCommands = new HashMap<>();
  private final Map<String, CompositeCommand> adminSubCommands = new HashMap<>();

  public CommandManagerImpl(AubadeCore core) {
    this.core = core;
    this.logger = core.getLogger();
    registerDefaults();
  }

  public void registerDefaults() {
    registerSubCommand("island", new DefaultPlayerCommand(core));
    registerSubCommand("island", new IslandCreateCommand(core));
    registerSubCommand("island", new IslandDeleteCommand(core));
    registerSubCommand("island", new IslandHomeCommand(core));
    registerSubCommand("island", new IslandSetHomeCommand(core));
    registerSubCommand("island", new IslandInviteCommand(core));
    registerSubCommand("island", new IslandAcceptCommand(core));
    registerSubCommand("island", new IslandKickCommand(core));
    registerSubCommand("island", new IslandLeaveCommand(core));
    registerSubCommand("island", new IslandInfoCommand(core));
    registerSubCommand("island", new IslandTrustCommand(core, false));
    registerSubCommand("island", new IslandTrustCommand(core, true));
    registerSubCommand("island", new IslandBanCommand(core, false));
    registerSubCommand("island", new IslandBanCommand(core, true));
    registerSubCommand("island", new IslandSettingsCommand(core));
    registerSubCommand("island", new IslandTopCommand(core));
    registerSubCommand("island", new IslandWarpCommand(core));
    registerSubCommand("island", new IslandSetWarpCommand(core));
    registerSubCommand("island", new IslandDelWarpCommand(core));
    registerSubCommand("isadmin", new DefaultAdminCommand());
    registerSubCommand("isadmin", new AdminReloadCommand(core));
    registerSubCommand("isadmin", new AdminPurgeCommand(core));
    registerSubCommand("isadmin", new AdminInfoCommand(core));
    registerSubCommand("isadmin", new AdminDeleteCommand(core));
    registerSubCommand("isadmin", new AdminSetSpawnCommand(core));
  }

  @Override
  public void registerSubCommand(String parentLabel, CompositeCommand subCommand) {
    if ("island".equalsIgnoreCase(parentLabel)) {
      playerSubCommands.put(subCommand.getLabel().toLowerCase(), subCommand);
    } else if ("isadmin".equalsIgnoreCase(parentLabel)) {
      adminSubCommands.put(subCommand.getLabel().toLowerCase(), subCommand);
    }
    logger.info("[命令] 已注册子命令: /" + parentLabel + " " + subCommand.getLabel());
  }

  public List<String> actions() {
    TreeSet<String> labels = new TreeSet<>();
    labels.addAll(playerSubCommands.keySet());
    labels.addAll(adminSubCommands.keySet());
    labels.add("admin");
    return new ArrayList<>(labels);
  }

  public boolean onCommand(CommandSender sender, String label, String[] args) {
    if (args.length == 0) {
      args = new String[]{"help"};
    }
    Map<String, CompositeCommand> subMap = selectSubCommandMap(args);
    String[] routed = normalizeArgs(args);
    String subLabel = routed[0].toLowerCase();
    CompositeCommand sub = subMap.get(subLabel);
    if (sub == null) {
      sender.sendMessage("§c未知子命令: " + subLabel + "，使用 /" + label + " help 查看帮助。");
      return true;
    }
    String[] subArgs = new String[routed.length - 1];
    System.arraycopy(routed, 1, subArgs, 0, subArgs.length);
    return sub.execute(sender, subArgs);
  }

  public List<String> onTabComplete(CommandSender sender, String[] args) {
    Map<String, CompositeCommand> subMap = selectSubCommandMap(args);
    String[] routed = normalizeArgs(args);
    if (routed.length == 0) {
      return List.copyOf(subMap.keySet());
    }
    if (routed.length == 1) {
      List<String> completions = new ArrayList<>();
      for (String key : subMap.keySet()) {
        if (key.startsWith(routed[0].toLowerCase())) {
          completions.add(key);
        }
      }
      return completions;
    }
    CompositeCommand sub = subMap.get(routed[0].toLowerCase());
    if (sub != null) {
      String[] subArgs = new String[routed.length - 1];
      System.arraycopy(routed, 1, subArgs, 0, subArgs.length);
      return sub.tabComplete(sender, subArgs);
    }
    return Collections.emptyList();
  }

  private Map<String, CompositeCommand> selectSubCommandMap(String[] args) {
    if (args.length > 0 && isAdminToken(args[0])) {
      return adminSubCommands;
    }
    return playerSubCommands;
  }

  private boolean isAdminToken(String value) {
    return "admin".equalsIgnoreCase(value) || "isadmin".equalsIgnoreCase(value);
  }

  private String[] normalizeArgs(String[] args) {
    if (args.length == 0) {
      return args;
    }
    if (isAdminToken(args[0])) {
      if (args.length == 1) {
        return new String[]{"help"};
      }
      String[] normalized = new String[args.length - 1];
      System.arraycopy(args, 1, normalized, 0, normalized.length);
      return normalized;
    }
    return args;
  }
}

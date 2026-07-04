package xuanmo.aubade.core.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.arcartxsuite.api.aubade.permission.Permission;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.features.checkmeout.CheckMeOutAddon;
import xuanmo.aubade.core.features.checkmeout.CheckMeOutAddon.SubmittedIsland;

/**
 * /island checkmeout — 提交/浏览岛屿展示。
 */
public class IslandCheckMeOutCommand extends CompositeCommand {

  private static final int DEFAULT_LIMIT = 10;

  private final AubadeCore core;

  public IslandCheckMeOutCommand(AubadeCore core) {
    super("checkmeout", "快来看看我", Permission.PLAYER, true);
    this.core = core;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (!checkPlayer(sender)) {
      sender.sendMessage("§c此命令只能由玩家执行。");
      return true;
    }
    if (!checkPermission(sender)) {
      return false;
    }

    Player player = (Player) sender;
    CheckMeOutAddon addon = getAddon();
    if (addon == null) {
      player.sendMessage("§c快来看看我功能暂不可用。");
      return true;
    }

    if (args.length == 0) {
      return submitOwnIsland(player, addon);
    }

    String sub = args[0].toLowerCase(Locale.ROOT);
    return switch (sub) {
      case "list" -> showList(player, addon);
      case "vote" -> vote(player, addon, args);
      default -> {
        player.sendMessage("§c未知子命令。可用：§e/island checkmeout §7、§e/island checkmeout list §7、§e/island checkmeout vote <玩家>");
        yield true;
      }
    };
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      String prefix = args[0].toLowerCase(Locale.ROOT);
      List<String> result = new ArrayList<>();
      for (String candidate : List.of("list", "vote")) {
        if (prefix.isEmpty() || candidate.startsWith(prefix)) {
          result.add(candidate);
        }
      }
      return result;
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("vote")) {
      String prefix = args[1].toLowerCase(Locale.ROOT);
      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> name != null && (prefix.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(prefix)))
          .sorted()
          .toList();
    }
    return List.of();
  }

  private boolean submitOwnIsland(Player player, CheckMeOutAddon addon) {
    Optional<Island> opt = core.getLifecycleManager().getIslandManager().getIslandByOwner(player.getUniqueId());
    if (opt.isEmpty()) {
      player.sendMessage("§c你还没有岛屿，无法提交展示。");
      return true;
    }
    Island island = opt.get();
    if (addon.isSubmitted(island.getUniqueId())) {
      player.sendMessage("§c你的岛屿已经在展示列表中了。");
      return true;
    }
    if (addon.submit(island.getUniqueId(), player.getName())) {
      player.sendMessage("§a已将你的岛屿提交到展示列表。可使用 §e/island checkmeout list §a查看推荐。");
    } else {
      player.sendMessage("§c提交失败，请稍后重试。");
    }
    return true;
  }

  private boolean showList(Player player, CheckMeOutAddon addon) {
    List<SubmittedIsland> leaderboard = addon.getLeaderboard(DEFAULT_LIMIT);
    if (leaderboard.isEmpty()) {
      player.sendMessage("§c当前没有任何展示中的岛屿。");
      return true;
    }

    player.sendMessage("§6========== 岛屿展示推荐 ==========");
    int rank = 1;
    for (SubmittedIsland submission : leaderboard) {
      Optional<Island> opt = core.getLifecycleManager().getIslandManager().getIslandById(submission.islandId());
      if (opt.isEmpty()) {
        continue;
      }
      Island island = opt.get();
      String islandName = island.getName() != null ? island.getName() : "未命名岛屿";
      String ownerName = displayOwnerName(island, submission.submitter());
      int votes = addon.getVotes(submission.islandId());
      player.sendMessage("§e#" + rank + " §f" + islandName + " §7(岛主: §b" + ownerName + "§7) §a票数: §e" + votes + " §7— §f/island visit " + ownerName);
      rank++;
    }
    if (rank == 1) {
      player.sendMessage("§c当前没有任何可显示的岛屿。");
    }
    player.sendMessage("§6=================================");
    return true;
  }

  private boolean vote(Player player, CheckMeOutAddon addon, String[] args) {
    if (args.length < 2) {
      player.sendMessage("§c用法: /island checkmeout vote <玩家>");
      return true;
    }
    String targetName = args[1];
    OfflinePlayer target = resolveTarget(targetName);
    if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
      player.sendMessage("§c目标玩家不存在: §e" + targetName);
      return true;
    }
    if (player.getUniqueId().equals(target.getUniqueId())) {
      player.sendMessage("§c不能给自己的岛屿投票。");
      return true;
    }

    Optional<Island> opt = core.getLifecycleManager().getIslandManager().getIslandByOwner(target.getUniqueId());
    if (opt.isEmpty()) {
      player.sendMessage("§c目标玩家没有岛屿。");
      return true;
    }
    Island island = opt.get();
    if (!addon.isSubmitted(island.getUniqueId())) {
      player.sendMessage("§c该岛屿还没有进入展示列表。");
      return true;
    }
    if (addon.hasVoted(island.getUniqueId(), player.getUniqueId())) {
      player.sendMessage("§c你已经给这个岛屿投过票了。");
      return true;
    }
    if (addon.vote(island.getUniqueId(), player.getUniqueId())) {
      player.sendMessage("§a你已给 §e" + displayOwnerName(island, targetName) + " §a的岛屿投票。");
    } else {
      player.sendMessage("§c投票失败。");
    }
    return true;
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

  private String displayOwnerName(Island island, String fallback) {
    OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
    if (owner.getName() != null) {
      return owner.getName();
    }
    return fallback;
  }

  private CheckMeOutAddon getAddon() {
    var addonLifecycleManager = core.getLifecycleManager().getAddonLifecycleManager();
    if (addonLifecycleManager == null) {
      return null;
    }
    var addon = addonLifecycleManager.getExtension("checkmeout");
    return addon instanceof CheckMeOutAddon checkMeOutAddon ? checkMeOutAddon : null;
  }
}

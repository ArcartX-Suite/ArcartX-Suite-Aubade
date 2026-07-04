package xuanmo.aubade.core.features.upgrades.command;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.arcartxsuite.api.aubade.permission.Permission;
import xuanmo.aubade.core.features.upgrades.UpgradesAddon;

/**
 * /island upgrade 子命令。
 */
public class UpgradeCommand extends CompositeCommand {

  private static final DecimalFormat MONEY = new DecimalFormat("0.##");

  private final UpgradesAddon addon;

  public UpgradeCommand(UpgradesAddon addon) {
    super("upgrade", "升级岛屿功能", Permission.PLAYER, true);
    this.addon = addon;
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
    Optional<Island> opt = addon.getIslandManager().getIslandByOwner(player.getUniqueId());
    if (opt.isEmpty()) {
      player.sendMessage("§c你还没有岛屿。");
      return true;
    }
    Island island = opt.get();

    if (args.length == 0) {
      sendStatus(player, island);
      return true;
    }

    String upgradeKey = args[0].toLowerCase(Locale.ROOT);
    if (!addon.getUpgradeConfigs().containsKey(upgradeKey)) {
      player.sendMessage("§c未知升级类型：§e" + upgradeKey);
      sendStatus(player, island);
      return true;
    }

    var cfg = addon.getUpgradeConfigs().get(upgradeKey);
    int currentLevel = addon.getLevel(island.getUniqueId(), upgradeKey);
    if (currentLevel >= cfg.maxLevel()) {
      player.sendMessage("§c" + addon.getUpgradeDisplayName(upgradeKey) + " 已经满级了。");
      return true;
    }

    double cost = addon.getNextCost(island.getUniqueId(), upgradeKey);
    if (cost < 0) {
      player.sendMessage("§c无法计算该升级价格。");
      return true;
    }

    Economy economy = getEconomy();
    if (economy == null) {
      player.sendMessage("§c经济系统不可用。");
      return true;
    }
    if (economy.getBalance(player) < cost) {
      player.sendMessage("§c你的余额不足，升级需要 §e" + formatMoney(cost) + " §c。");
      return true;
    }

    EconomyResponse withdraw = economy.withdrawPlayer(player, cost);
    if (!withdraw.transactionSuccess()) {
      player.sendMessage("§c扣费失败：" + withdraw.errorMessage);
      return true;
    }

    if (!addon.upgrade(island.getUniqueId(), upgradeKey)) {
      economy.depositPlayer(player, cost);
      player.sendMessage("§c升级失败，请稍后重试。");
      return true;
    }

    player.sendMessage("§a升级成功：§e" + addon.getUpgradeDisplayName(upgradeKey)
        + " §a已提升到 §eLv." + addon.getLevel(island.getUniqueId(), upgradeKey)
        + " §a，当前值 §e" + addon.getCurrentValue(island, upgradeKey)
        + " §a。");
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length > 1) {
      return List.of();
    }
    String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
    List<String> result = new ArrayList<>();
    for (String key : addon.getUpgradeTypes()) {
      if (prefix.isEmpty() || key.startsWith(prefix)) {
        result.add(key);
      }
    }
    result.sort(String::compareTo);
    return result;
  }

  private void sendStatus(Player player, Island island) {
    player.sendMessage("§a可用升级：");
    List<String> keys = new ArrayList<>(addon.getUpgradeTypes());
    keys.sort(String::compareTo);
    for (String key : keys) {
      var cfg = addon.getUpgradeConfigs().get(key);
      int level = addon.getLevel(island.getUniqueId(), key);
      int currentValue = addon.getCurrentValue(island, key);
      double nextCost = addon.getNextCost(island.getUniqueId(), key);
      String costText = nextCost < 0 ? "§a已满级" : "§e" + formatMoney(nextCost);
      player.sendMessage("§7- §e" + addon.getUpgradeDisplayName(key)
          + " §7(" + key + ") §fLv." + level + "/" + cfg.maxLevel()
          + " §7当前值：§a" + currentValue
          + " §7下一级花费：" + costText);
    }
    player.sendMessage("§7使用 §e/island upgrade <type> §7执行升级。");
  }

  private Economy getEconomy() {
    RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
    return provider != null ? provider.getProvider() : null;
  }

  private String formatMoney(double value) {
    return MONEY.format(value);
  }
}

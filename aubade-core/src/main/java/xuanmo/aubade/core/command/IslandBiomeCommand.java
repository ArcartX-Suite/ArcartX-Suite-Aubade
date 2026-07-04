package xuanmo.aubade.core.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xuanmo.arcartxsuite.api.aubade.command.CompositeCommand;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.arcartxsuite.api.aubade.permission.Permission;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.features.biomes.BiomesAddon;

/**
 * /island biome <群系> — 切换自己岛屿的生物群系。
 */
public class IslandBiomeCommand extends CompositeCommand {

  private final AubadeCore core;

  public IslandBiomeCommand(AubadeCore core) {
    super("biome", "切换岛屿生物群系", Permission.PLAYER, true);
    this.core = core;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (!checkPlayer(sender)) {
      sender.sendMessage("§c此命令只能由玩家执行。");
      return true;
    }

    Player player = (Player) sender;
    BiomesAddon addon = getAddon();
    if (addon == null) {
      player.sendMessage("§c生物群系功能暂不可用。");
      return true;
    }

    if (args.length == 0) {
      sendUsage(player, addon);
      return true;
    }

    String biomeKey = args[0].toLowerCase(Locale.ROOT);
    if (!addon.getAllowedBiomes().contains(biomeKey)) {
      player.sendMessage("§c当前不支持该生物群系: §e" + biomeKey);
      sendUsage(player, addon);
      return true;
    }

    Biome biome;
    try {
      biome = Biome.valueOf(biomeKey.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      player.sendMessage("§c无效的生物群系: §e" + biomeKey);
      sendUsage(player, addon);
      return true;
    }

    Optional<Island> opt = core.getLifecycleManager().getIslandManager().getIslandByOwner(player.getUniqueId());
    if (opt.isEmpty()) {
      player.sendMessage("§c你还没有岛屿。");
      return true;
    }
    Island island = opt.get();
    if (!island.inProtectionRange(player.getLocation())) {
      player.sendMessage("§c你只能在自己的岛屿范围内切换生物群系。");
      return true;
    }

    if (addon.changeBiomeSync(island, biome.name())) {
      player.sendMessage("§a已将你的岛屿生物群系切换为 §e" + biome.name().toLowerCase(Locale.ROOT) + "§a。");
    } else {
      player.sendMessage("§c切换失败，请检查群系是否可用或区块是否加载。");
    }
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] args) {
    String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
    List<String> result = new ArrayList<>();
    for (String biome : getAddonBiomes()) {
      if (prefix.isEmpty() || biome.startsWith(prefix)) {
        result.add(biome);
      }
    }
    return result;
  }

  private void sendUsage(Player player, BiomesAddon addon) {
    player.sendMessage("§6可用群系: §f" + String.join("§7, §f", addon.getAllowedBiomes()));
    player.sendMessage("§e用法: §f/island biome <群系>");
  }

  private List<String> getAddonBiomes() {
    BiomesAddon addon = getAddon();
    return addon != null ? addon.getAllowedBiomes() : List.of();
  }

  private BiomesAddon getAddon() {
    var addonLifecycleManager = core.getLifecycleManager().getAddonLifecycleManager();
    if (addonLifecycleManager == null) {
      return null;
    }
    var addon = addonLifecycleManager.getExtension("biomes");
    return addon instanceof BiomesAddon biomesAddon ? biomesAddon : null;
  }
}

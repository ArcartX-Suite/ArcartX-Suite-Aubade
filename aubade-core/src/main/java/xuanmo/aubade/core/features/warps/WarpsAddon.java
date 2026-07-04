package xuanmo.aubade.core.features.warps;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import xuanmo.arcartxsuite.api.aubade.addon.AddonDescriptor;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.command.IslandDelWarpCommand;
import xuanmo.aubade.core.command.IslandSetWarpCommand;
import xuanmo.aubade.core.command.IslandWarpCommand;
import xuanmo.aubade.core.features.AbstractFeatureAddon;

/**
 * 传送牌功能组件。
 * 提供岛屿内传送牌的创建、删除和公共传送面板。
 */
public class WarpsAddon extends AbstractFeatureAddon {

  private final Map<UUID, List<Warp>> islandWarps = new HashMap<>();

  public WarpsAddon(AubadeCore core) {
    super(core, AddonDescriptor.builder("warps")
        .name("传送牌")
        .version("1.0.0")
        .mainClass(WarpsAddon.class.getName())
        .build());
  }

  @Override
  public String getFeatureId() {
    return "warps";
  }

  @Override
  public String getFriendlyName() {
    return "传送牌";
  }

  @Override
  public void onEnable() {
    super.onEnable();
    try {
      getCommandManager().registerSubCommand("island", new IslandWarpCommand(core));
      getCommandManager().registerSubCommand("island", new IslandSetWarpCommand(core));
      getCommandManager().registerSubCommand("island", new IslandDelWarpCommand(core));
    } catch (Exception e) {
      core.getLogger().warning("[Warps] 注册命令失败: " + e.getMessage());
    }
    registerUi("warp_board.yml", "warp_board");
    core.getLogger().info("[Warps] 传送牌组件已启用。");
  }

  @Override
  public void onDisable() {
    super.onDisable();
    islandWarps.clear();
    core.getLogger().info("[Warps] 传送牌组件已禁用。");
  }

  public List<Warp> getWarps(UUID islandId) {
    return new ArrayList<>(islandWarps.getOrDefault(islandId, List.of()));
  }

  public Warp getWarp(UUID islandId, String name) {
    List<Warp> warps = islandWarps.get(islandId);
    if (warps == null) {
      return null;
    }
    for (Warp warp : warps) {
      if (warp.getName().equalsIgnoreCase(name)) {
        return warp;
      }
    }
    return null;
  }

  public void addWarp(Warp warp) {
    islandWarps.computeIfAbsent(warp.getIslandId(), k -> new ArrayList<>()).add(warp);
  }

  public boolean removeWarp(UUID islandId, String name) {
    List<Warp> warps = islandWarps.get(islandId);
    if (warps == null) {
      return false;
    }
    return warps.removeIf(w -> w.getName().equalsIgnoreCase(name));
  }

  private void registerUi(String fileName, String uiId) {
    File uiDir = new File(core.getDataFolder(), "arcartx/ui");
    File uiFile = new File(uiDir, fileName);
    if (!uiFile.exists()) {
      core.saveResource("arcartx/ui/" + fileName, false);
    }
    getUiManager().registerUi(uiId, uiId, uiFile);
  }
}


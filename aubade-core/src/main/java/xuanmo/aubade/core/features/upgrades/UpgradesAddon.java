package xuanmo.aubade.core.features.upgrades;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import xuanmo.arcartxsuite.api.aubade.addon.AddonDescriptor;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.features.AbstractExtensionAddon;
import xuanmo.aubade.core.features.upgrades.command.UpgradeCommand;
import xuanmo.aubade.core.island.IslandManagerImpl;

public class UpgradesAddon extends AbstractExtensionAddon {

  private final Map<String, UpgradeConfig> upgradeConfigs = new HashMap<>();
  private final Map<UUID, Map<String, Integer>> islandUpgrades = new HashMap<>();

  public UpgradesAddon(AubadeCore core) {
    super(core, AddonDescriptor.builder("upgrades")
        .name("岛屿升级")
        .version("1.0.0")
        .mainClass(UpgradesAddon.class.getName())
        .build());
  }

  @Override
  public String getExtensionId() {
    return "upgrades";
  }

  @Override
  public String getFriendlyName() {
    return "岛屿升级";
  }

  @Override
  public void onLoad() {
    File configFile = new File(core.getDataFolder(), "features/upgrades.yml");
    if (!configFile.exists()) {
      core.saveResource("features/upgrades.yml", false);
    }
    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    loadUpgradeConfig("protection_range", config);
    loadUpgradeConfig("member_limit", config);
  }

  @Override
  public void onEnable() {
    super.onEnable();
    try {
      getCommandManager().registerSubCommand("island", new UpgradeCommand(this));
    } catch (Exception e) {
      core.getLogger().warning("[Upgrades] 注册命令失败: " + e.getMessage());
    }
    syncIslandUpgradeState();
    core.getLogger().info("[Upgrades] 岛屿升级扩展已启用。");
  }

  @Override
  public void onDisable() {
    super.onDisable();
  }

  @Override
  public void onReload() {
    upgradeConfigs.clear();
    onLoad();
    syncIslandUpgradeState();
  }

  private void loadUpgradeConfig(String key, YamlConfiguration config) {
    int maxLevel = config.getInt(key + ".max-level", 5);
    double baseCost = config.getDouble(key + ".base-cost", 1000);
    double costMultiplier = config.getDouble(key + ".cost-multiplier", 1.5);
    int baseValue = config.getInt(key + ".base-value", 50);
    int valueIncrement = config.getInt(key + ".value-increment", 10);
    upgradeConfigs.put(key, new UpgradeConfig(maxLevel, baseCost, costMultiplier, baseValue, valueIncrement));
  }

  public int getLevel(UUID islandId, String upgradeKey) {
    Map<String, Integer> upgrades = islandUpgrades.computeIfAbsent(islandId, k -> new HashMap<>());
    Integer cached = upgrades.get(upgradeKey);
    if (cached != null) {
      return cached;
    }
    Optional<Island> opt = getIslandManager().getIslandById(islandId);
    if (opt.isEmpty()) {
      return 0;
    }
    Island island = opt.get();
    int level = parseLevel(island.getMeta().get(levelMetaKey(upgradeKey)));
    upgrades.put(upgradeKey, level);
    return level;
  }

  public int getValue(UUID islandId, String upgradeKey) {
    UpgradeConfig cfg = upgradeConfigs.get(upgradeKey);
    if (cfg == null) {
      return 0;
    }
    int level = getLevel(islandId, upgradeKey);
    return cfg.baseValue + (level * cfg.valueIncrement);
  }

  public double getNextCost(UUID islandId, String upgradeKey) {
    UpgradeConfig cfg = upgradeConfigs.get(upgradeKey);
    if (cfg == null) {
      return -1;
    }
    int currentLevel = getLevel(islandId, upgradeKey);
    if (currentLevel >= cfg.maxLevel) {
      return -1;
    }
    return cfg.baseCost * Math.pow(cfg.costMultiplier, currentLevel);
  }

  public boolean upgrade(UUID islandId, String upgradeKey) {
    UpgradeConfig cfg = upgradeConfigs.get(upgradeKey);
    if (cfg == null) {
      return false;
    }
    int currentLevel = getLevel(islandId, upgradeKey);
    if (currentLevel >= cfg.maxLevel) {
      return false;
    }
    Optional<Island> opt = getIslandManager().getIslandById(islandId);
    if (opt.isEmpty()) {
      return false;
    }
    Island island = opt.get();
    int nextLevel = currentLevel + 1;
    islandUpgrades.computeIfAbsent(islandId, k -> new HashMap<>()).put(upgradeKey, nextLevel);
    island.getMeta().put(levelMetaKey(upgradeKey), String.valueOf(nextLevel));
    applyUpgradeValue(island, upgradeKey);
    getIslandManager().saveIsland(island);
    return true;
  }

  public Map<String, UpgradeConfig> getUpgradeConfigs() {
    return new HashMap<>(upgradeConfigs);
  }

  public Set<String> getUpgradeTypes() {
    return Set.copyOf(upgradeConfigs.keySet());
  }

  public String getUpgradeDisplayName(String upgradeKey) {
    return switch (upgradeKey) {
      case "protection_range" -> "保护范围";
      case "member_limit" -> "成员上限";
      default -> upgradeKey;
    };
  }

  public int getCurrentValue(Island island, String upgradeKey) {
    return switch (upgradeKey) {
      case "protection_range" -> island.getProtectionRange();
      case "member_limit" -> parseLevel(island.getMeta().get(memberLimitMetaKey()));
      default -> getValue(island.getUniqueId(), upgradeKey);
    };
  }

  public int getMemberLimit(Island island) {
    int limit = parseLevel(island.getMeta().get(memberLimitMetaKey()));
    return limit > 0 ? limit : Integer.MAX_VALUE;
  }

  public void applyUpgradeValue(Island island, String upgradeKey) {
    int value = getValue(island.getUniqueId(), upgradeKey);
    switch (upgradeKey) {
      case "protection_range" -> island.setProtectionRange(value);
      case "member_limit" -> island.getMeta().put(memberLimitMetaKey(), String.valueOf(value));
      default -> island.getMeta().put("upgrade." + upgradeKey + ".value", String.valueOf(value));
    }
  }

  public int parseLevel(String raw) {
    if (raw == null || raw.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  public String levelMetaKey(String upgradeKey) {
    return "upgrade." + upgradeKey + ".level";
  }

  public String memberLimitMetaKey() {
    return "member_limit";
  }

  private void syncIslandUpgradeState() {
    if (!(core.getIslandManager() instanceof IslandManagerImpl manager)) {
      return;
    }
    for (Island island : manager.getCachedIslands()) {
      boolean dirty = false;
      if (!island.getMeta().containsKey(levelMetaKey("protection_range"))) {
        island.getMeta().put(levelMetaKey("protection_range"), "0");
        dirty = true;
      }
      if (!island.getMeta().containsKey(levelMetaKey("member_limit"))) {
        island.getMeta().put(levelMetaKey("member_limit"), "0");
        dirty = true;
      }
      if (!island.getMeta().containsKey(memberLimitMetaKey())) {
        island.getMeta().put(memberLimitMetaKey(), String.valueOf(getValue(island.getUniqueId(), "member_limit")));
        dirty = true;
      }
      if (dirty) {
        getIslandManager().saveIsland(island);
      }
    }
  }

  public record UpgradeConfig(int maxLevel, double baseCost, double costMultiplier, int baseValue, int valueIncrement) {
  }
}


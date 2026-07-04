package xuanmo.aubade.core.features.biomes;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import xuanmo.arcartxsuite.api.aubade.addon.AddonDescriptor;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.command.IslandBiomeCommand;
import xuanmo.aubade.core.features.AbstractFeatureAddon;

/**
 * 岛屿生物群系功能组件。
 * 提供岛屿范围内生物群系更换与可视化预览。
 */
public class BiomesAddon extends AbstractFeatureAddon {

  private static final List<String> ALLOWED_BIOMES = Arrays.asList(
      "plains", "desert", "savanna", "jungle", "forest", "birch_forest", "dark_forest",
      "taiga", "snowy_taiga", "beach", "mushroom_fields", "ocean", "deep_ocean",
      "swamp", "savanna_plateau", "meadow", "cherry_grove", "badlands", "ice_spikes"
  );

  public BiomesAddon(AubadeCore core) {
    super(core, AddonDescriptor.builder("biomes")
        .name("生物群系")
        .version("1.0.0")
        .mainClass(BiomesAddon.class.getName())
        .build());
  }

  @Override
  public String getFeatureId() {
    return "biomes";
  }

  @Override
  public String getFriendlyName() {
    return "生物群系";
  }

  @Override
  public void onEnable() {
    super.onEnable();
    try {
      getCommandManager().registerSubCommand("island", new IslandBiomeCommand(core));
    } catch (Exception e) {
      core.getLogger().warning("[Biomes] 注册命令失败: " + e.getMessage());
    }
    registerUi("biome_selector.yml", "biome_selector");
    core.getLogger().info("[Biomes] 生物群系组件已启用。");
  }

  @Override
  public void onDisable() {
    super.onDisable();
    core.getLogger().info("[Biomes] 生物群系组件已禁用。");
  }

  public List<String> getAllowedBiomes() {
    return ALLOWED_BIOMES;
  }

  /**
   * 将岛屿保护范围内的区块生物群系替换为目标生物群系。
   */
  public CompletableFuture<Boolean> changeBiomeAsync(Island island, String biomeKey) {
    return CompletableFuture.supplyAsync(() -> changeBiomeSync(island, biomeKey));
  }

  public boolean changeBiomeSync(Island island, String biomeKey) {
    Biome target;
    try {
      target = Biome.valueOf(biomeKey.toUpperCase());
    } catch (IllegalArgumentException e) {
      core.getLogger().warning("[Biomes] 未知生物群系: " + biomeKey);
      return false;
    }

    if (island == null || island.getCenter() == null || island.getWorld() == null) {
      return false;
    }

    World world = island.getWorld();
    Location center = island.getCenter();
    int range = island.getProtectionRange();

    int minX = center.getBlockX() - range;
    int maxX = center.getBlockX() + range;
    int minZ = center.getBlockZ() - range;
    int maxZ = center.getBlockZ() + range;
    int minY = world.getMinHeight();
    int maxY = world.getMaxHeight();

    int chunkMinX = minX >> 4;
    int chunkMaxX = maxX >> 4;
    int chunkMinZ = minZ >> 4;
    int chunkMaxZ = maxZ >> 4;

    for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
      for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
        if (!world.isChunkLoaded(cx, cz)) {
          continue;
        }
        Chunk chunk = world.getChunkAt(cx, cz);
        int baseX = cx << 4;
        int baseZ = cz << 4;
        for (int x = 0; x < 16; x++) {
          int worldX = baseX + x;
          if (worldX < minX || worldX > maxX) {
            continue;
          }
          for (int z = 0; z < 16; z++) {
            int worldZ = baseZ + z;
            if (worldZ < minZ || worldZ > maxZ) {
              continue;
            }
            for (int y = minY; y <= maxY; y++) {
              world.setBiome(worldX, y, worldZ, target);
            }
          }
        }
      }
    }

    // 刷新区块
    for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
      for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
        if (world.isChunkLoaded(cx, cz)) {
          world.refreshChunk(cx, cz);
        }
      }
    }

    core.getLogger().info("[Biomes] 已将岛屿 " + island.getUniqueId() + " 生物群系更改为 " + biomeKey);
    return true;
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


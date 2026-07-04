package xuanmo.aubade.core.island;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.World;
import xuanmo.arcartxsuite.api.aubade.island.Island;

/**
 * 岛屿网格计算器。
 * 负责将岛屿索引转换为世界坐标，并维护间距保护。
 */
public class IslandGrid {

  private final int islandSpacing;
  private final AtomicInteger nextIndex = new AtomicInteger(0);
  private final Map<String, Map<Long, Island>> islandsByChunk = new ConcurrentHashMap<>();

  public IslandGrid(int islandSpacing) {
    this.islandSpacing = islandSpacing;
  }

  /**
   * 分配下一个可用的网格索引。
   */
  public int nextIndex() {
    return nextIndex.getAndIncrement();
  }

  /**
   * 根据网格索引计算世界坐标。
   * 采用螺旋展开方式：0→(0,0), 1→(1,0), 2→(1,1), 3→(0,1), 4→(-1,1), ...
   *
   * @param index 网格索引
   * @return 中心点坐标 (x, z)
   */
  public int[] getCenter(int index) {
    if (index == 0) {
      return new int[]{0, 0};
    }

    int layer = (int) Math.ceil((Math.sqrt(index + 1) - 1) / 2);
    int sideLen = layer * 2 + 1;
    int minIdx = (sideLen - 2) * (sideLen - 2);
    int offset = index - minIdx;

    int x, z;
    int perimeter = sideLen - 1;
    if (offset < perimeter) {
      x = layer;
      z = -layer + 1 + offset;
    } else if (offset < perimeter * 2) {
      x = layer - 1 - (offset - perimeter);
      z = layer;
    } else if (offset < perimeter * 3) {
      x = -layer;
      z = layer - 1 - (offset - perimeter * 2);
    } else {
      x = -layer + 1 + (offset - perimeter * 3);
      z = -layer;
    }

    return new int[]{x * islandSpacing, z * islandSpacing};
  }

  /**
   * 创建岛屿中心位置。
   */
  public Location createLocation(World world, int index) {
    int[] center = getCenter(index);
    return new Location(world, center[0], 128, center[1]);
  }

  /**
   * 注册岛屿的空间索引。
   */
  public void registerIsland(Island island) {
    if (island == null || island.getCenter() == null || island.getCenter().getWorld() == null) {
      return;
    }
    World world = island.getCenter().getWorld();
    String worldName = world.getName();
    int minChunkX = Math.floorDiv(island.getCenter().getBlockX() - island.getProtectionRange(), 16);
    int maxChunkX = Math.floorDiv(island.getCenter().getBlockX() + island.getProtectionRange(), 16);
    int minChunkZ = Math.floorDiv(island.getCenter().getBlockZ() - island.getProtectionRange(), 16);
    int maxChunkZ = Math.floorDiv(island.getCenter().getBlockZ() + island.getProtectionRange(), 16);
    Map<Long, Island> worldIndex = islandsByChunk.computeIfAbsent(worldName, key -> new ConcurrentHashMap<>());
    for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
      for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
        worldIndex.put(chunkKey(chunkX, chunkZ), island);
      }
    }
  }

  /**
   * 移除岛屿的空间索引。
   */
  public void unregisterIsland(Island island) {
    if (island == null || island.getCenter() == null || island.getCenter().getWorld() == null) {
      return;
    }
    World world = island.getCenter().getWorld();
    Map<Long, Island> worldIndex = islandsByChunk.get(world.getName());
    if (worldIndex == null) {
      return;
    }
    int minChunkX = Math.floorDiv(island.getCenter().getBlockX() - island.getProtectionRange(), 16);
    int maxChunkX = Math.floorDiv(island.getCenter().getBlockX() + island.getProtectionRange(), 16);
    int minChunkZ = Math.floorDiv(island.getCenter().getBlockZ() - island.getProtectionRange(), 16);
    int maxChunkZ = Math.floorDiv(island.getCenter().getBlockZ() + island.getProtectionRange(), 16);
    for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
      for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
        long key = chunkKey(chunkX, chunkZ);
        Island indexed = worldIndex.get(key);
        if (indexed != null && indexed.getUniqueId().equals(island.getUniqueId())) {
          worldIndex.remove(key);
        }
      }
    }
    if (worldIndex.isEmpty()) {
      islandsByChunk.remove(world.getName());
    }
  }

  /**
   * 通过坐标查找岛屿。
   */
  public Optional<Island> getIslandAt(Location location) {
    if (location == null || location.getWorld() == null) {
      return Optional.empty();
    }
    Map<Long, Island> worldIndex = islandsByChunk.get(location.getWorld().getName());
    if (worldIndex == null) {
      return Optional.empty();
    }
    Island island = worldIndex.get(chunkKey(location.getBlockX() >> 4, location.getBlockZ() >> 4));
    if (island == null) {
      return Optional.empty();
    }
    return island.inProtectionRange(location) ? Optional.of(island) : Optional.empty();
  }

  /**
   * 设置下一个索引（从数据库加载已有岛屿后使用）。
   */
  public void setNextIndex(int index) {
    nextIndex.set(index);
  }

  private long chunkKey(int chunkX, int chunkZ) {
    return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
  }
}

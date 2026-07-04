package xuanmo.aubade.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import xuanmo.aubade.core.config.CoreConfig;
import xuanmo.aubade.core.command.CommandManagerImpl;
import xuanmo.aubade.core.island.IslandManagerImpl;
import xuanmo.aubade.core.lifecycle.AddonLifecycleManager;
import xuanmo.aubade.core.lifecycle.CoreLifecycleManager;
import xuanmo.aubade.core.player.PlayerManagerImpl;
import xuanmo.aubade.core.storage.StorageManager;
import xuanmo.aubade.core.ui.UiManager;
import xuanmo.aubade.core.world.WorldManagerImpl;
import xuanmo.arcartxsuite.api.bridge.PacketBridgeAPI;

public final class AubadeCore {

  private static AubadeCore instance;

  private final JavaPlugin plugin;
  private final Logger logger;
  private final File dataFolder;
  private final ClassLoader resourceLoader;
  private final @Nullable PacketBridgeAPI packetBridge;

  private CoreLifecycleManager lifecycleManager;
  private CoreConfig coreConfig;
  private StorageManager storageManager;
  private AddonLifecycleManager addonLifecycleManager;
  private CommandManagerImpl commandManager;
  private IslandManagerImpl islandManager;
  private PlayerManagerImpl playerManager;
  private WorldManagerImpl worldManager;
  private UiManager uiManager;

  public AubadeCore(JavaPlugin plugin, File dataFolder, ClassLoader resourceLoader, @Nullable PacketBridgeAPI packetBridge) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.dataFolder = dataFolder;
    this.resourceLoader = resourceLoader;
    this.packetBridge = packetBridge;
  }

  public static AubadeCore get() {
    if (instance == null) {
      throw new IllegalStateException("AubadeCore 尚未初始化");
    }
    return instance;
  }

  public static void set(@Nullable AubadeCore core) {
    instance = core;
  }

  public JavaPlugin plugin() {
    return plugin;
  }

  public Logger logger() {
    return logger;
  }

  public File dataFolder() {
    return dataFolder;
  }

  public @Nullable PacketBridgeAPI packetBridge() {
    return packetBridge;
  }

  public CoreLifecycleManager lifecycleManager() {
    return lifecycleManager;
  }

  public void lifecycleManager(CoreLifecycleManager lifecycleManager) {
    this.lifecycleManager = lifecycleManager;
  }

  public CoreConfig coreConfig() {
    return coreConfig;
  }

  public void coreConfig(CoreConfig coreConfig) {
    this.coreConfig = coreConfig;
  }

  public StorageManager storageManager() {
    return storageManager;
  }

  public void storageManager(StorageManager storageManager) {
    this.storageManager = storageManager;
  }

  public AddonLifecycleManager addonLifecycleManager() {
    return addonLifecycleManager;
  }

  public void addonLifecycleManager(AddonLifecycleManager addonLifecycleManager) {
    this.addonLifecycleManager = addonLifecycleManager;
  }

  public CommandManagerImpl commandManager() {
    return commandManager;
  }

  public void commandManager(CommandManagerImpl commandManager) {
    this.commandManager = commandManager;
  }

  public IslandManagerImpl islandManager() {
    return islandManager;
  }

  public void islandManager(IslandManagerImpl islandManager) {
    this.islandManager = islandManager;
  }

  public PlayerManagerImpl playerManager() {
    return playerManager;
  }

  public void playerManager(PlayerManagerImpl playerManager) {
    this.playerManager = playerManager;
  }

  public WorldManagerImpl worldManager() {
    return worldManager;
  }

  public void worldManager(WorldManagerImpl worldManager) {
    this.worldManager = worldManager;
  }

  public UiManager uiManager() {
    return uiManager;
  }

  public void uiManager(UiManager uiManager) {
    this.uiManager = uiManager;
  }

  public Logger getLogger() {
    return logger();
  }

  public File getDataFolder() {
    return dataFolder();
  }

  public ClassLoader getResourceLoader() {
    return resourceLoader;
  }

  public Server getServer() {
    return plugin.getServer();
  }

  public void saveResource(String resourcePath, boolean replace) {
    File target = new File(dataFolder, resourcePath);
    if (target.exists() && !replace) {
      return;
    }
    if (target.getParentFile() != null && !target.getParentFile().exists()) {
      target.getParentFile().mkdirs();
    }
    try (InputStream input = resourceLoader.getResourceAsStream(resourcePath)) {
      if (input == null) {
        throw new IllegalArgumentException("Resource not found: " + resourcePath);
      }
      try (FileOutputStream output = new FileOutputStream(target)) {
        input.transferTo(output);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to save resource " + resourcePath, e);
    }
  }

  public CoreLifecycleManager getLifecycleManager() {
    return lifecycleManager();
  }

  public CoreConfig getCoreConfig() {
    return coreConfig();
  }

  public StorageManager getStorageManager() {
    return storageManager();
  }

  public AddonLifecycleManager getAddonLifecycleManager() {
    return addonLifecycleManager();
  }

  public CommandManagerImpl getCommandManager() {
    return commandManager();
  }

  public IslandManagerImpl getIslandManager() {
    return islandManager();
  }

  public PlayerManagerImpl getPlayerManager() {
    return playerManager();
  }

  public WorldManagerImpl getWorldManager() {
    return worldManager();
  }

  public UiManager getUiManager() {
    return uiManager();
  }
}

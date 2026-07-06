
package xuanmo.aubade.core;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xuanmo.aubade.core.command.CommandManagerImpl;
import xuanmo.aubade.core.config.CoreConfig;
import xuanmo.aubade.core.integration.AubadePlaceholderExpansion;
import xuanmo.aubade.core.lifecycle.CoreLifecycleManager;
import xuanmo.aubade.core.storage.StorageManager;
import xuanmo.aubade.core.sync.CrossServerIslandSyncService;
import xuanmo.aubade.core.sync.DisabledIslandSyncService;
import xuanmo.aubade.core.sync.IslandSyncService;
import xuanmo.aubade.core.ui.packet.AdminUiPacketDispatcher;
import xuanmo.arcartxsuite.api.AbstractAXSModule;
import xuanmo.arcartxsuite.api.ClientPacketHandler;
import xuanmo.arcartxsuite.api.ModuleCommandHandler;
import xuanmo.arcartxsuite.api.ModuleDescriptor;
import xuanmo.arcartxsuite.api.capability.DatabaseMigratable;

public final class AubadeModule extends AbstractAXSModule implements ModuleCommandHandler {

  static final List<String> CORE_UI_FILES = List.of(
      "aubade_main.yml",
      "aubade_admin.yml",
      "aubade_top.yml",
      "aubade_create.yml",
      "aubade_invite.yml",
      "level_display.yml",
      "level_top.yml",
      "member_manage.yml",
      "team_settings.yml",
      "challenges_list.yml",
      "challenge_detail.yml",
      "island_bank.yml",
      "warp_board.yml",
      "biome_selector.yml",
      "border_settings.yml",
      "island_rename.yml",
      "team_transfer.yml",
      "warp_create.yml"
  );

  private CoreConfig coreConfig;
  private AubadeCore core;
  private CommandManagerImpl commandManager;
  private IslandSyncService islandSyncService = DisabledIslandSyncService.INSTANCE;

  @Override
  public ModuleDescriptor descriptor() {
    return ModuleDescriptor.builder("aubade")
        .name("Aubade")
        .version("1.0.0")
        .mainClass(getClass().getName())
        .externalSoftDepends(List.of("PlaceholderAPI", "Vault"))
        .build();
  }

  @Override
  protected @Nullable String configFileName() {
    return "config.yml";
  }

  @Override
  protected @Nullable String messagesFileName() {
    return "messages.yml";
  }

  @Override
  protected @NotNull Map<String, String> uiResourceMappings() {
    Map<String, String> mappings = new LinkedHashMap<>();
    for (String fileName : CORE_UI_FILES) {
      mappings.put("arcartx/ui/" + fileName, "arcartx/ui/" + fileName);
    }
    return mappings;
  }

  @Override
  protected void loadConfiguration(@Nullable File configFile) {
    if (configFile != null) {
      this.coreConfig = new CoreConfig(configFile, logger);
      this.coreConfig.load();
    }
  }

  @Override
  protected void startService() throws Exception {
    JavaPlugin plugin = this.plugin;
    this.core = new AubadeCore(plugin, dataFolder, getClass().getClassLoader(), packetBridge);
    AubadeCore.set(core);
    core.coreConfig(coreConfig);
    this.islandSyncService = createIslandSyncService();
    core.islandSyncService(islandSyncService);
    this.commandManager = new CommandManagerImpl(core);
    core.commandManager(commandManager);
    core.lifecycleManager(new CoreLifecycleManager(core));
    core.getLifecycleManager().onEnable();
    islandSyncService.start();
    registerCoreUis();
    registerCapability(DatabaseMigratable.class, new DatabaseMigratable() {
      @Override
      public @NotNull String moduleId() {
        return "aubade";
      }

      @Override
      public @NotNull xuanmo.arcartxsuite.api.storage.MigrationResult migrateDatabase(@NotNull xuanmo.arcartxsuite.api.storage.StorageDescriptor targetDescriptor, boolean overwriteTarget) {
        return new xuanmo.arcartxsuite.api.storage.MigrationResult(false, 0, 0);
      }

      @Override
      public @NotNull xuanmo.arcartxsuite.api.storage.StorageDescriptor currentDescriptor() {
        return core.getStorageManager() != null ? core.getStorageManager().getDescriptor() : coreConfig.getStorageDescriptor();
      }
    });
  }

  @Override
  protected void stopService() {
    if (islandSyncService != null) {
      islandSyncService.close();
    }
    if (core != null && core.getLifecycleManager() != null) {
      core.getLifecycleManager().onDisable();
    }
    if (core != null) {
      core.islandSyncService(null);
    }
    AubadeCore.set(null);
    core = null;
  }

  private IslandSyncService createIslandSyncService() {
    if (core == null || coreConfig == null || !coreConfig.isSyncEnabled() || crossServer == null) {
      return DisabledIslandSyncService.INSTANCE;
    }
    return new CrossServerIslandSyncService(core, crossServer, coreConfig.getSyncChannel());
  }

  @Override
  protected @NotNull List<Listener> createListeners() {
    return List.of();
  }

  @Override
  protected @Nullable Object createPlaceholderExpansion() {
    return core != null ? new AubadePlaceholderExpansion(core) : null;
  }

  @Override
  protected @Nullable ClientPacketHandler createPacketHandler() {
    if (core == null || core.getUiManager() == null || core.getLifecycleManager() == null) {
      return null;
    }
    return new AdminUiPacketDispatcher(core);
  }

  private void registerCoreUis() {
    if (core == null || core.getUiManager() == null) {
      return;
    }
    for (String fileName : CORE_UI_FILES) {
      registerUi(fileName.substring(0, fileName.length() - 4), fileName.substring(0, fileName.length() - 4));
    }
  }

  private void registerUi(String fileName, String uiId) {
    File uiDir = new File(core.getDataFolder(), "arcartx/ui");
    File uiFile = new File(uiDir, fileName + ".yml");
    if (!uiFile.exists()) {
      core.saveResource("arcartx/ui/" + fileName + ".yml", false);
    }
    core.getUiManager().registerUi(uiId, uiId, uiFile);
  }

  @Override
  protected @NotNull Map<String, TabExecutor> commandBindings() {
    return Map.of();
  }

  @Override
  public String commandId() {
    return "aubade";
  }

  @Override
  public List<String> commandAliases() {
    return List.of("island", "is");
  }

  @Override
  public List<String> actions() {
    return commandManager != null ? commandManager.actions() : ModuleCommandHandler.super.actions();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
    return commandManager != null && commandManager.onCommand(sender, label, stripModuleToken(args));
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
    return commandManager != null ? commandManager.onTabComplete(sender, stripModuleToken(args)) : null;
  }

  /**
   * 宿主约定 {@code args[0]} 为模块 id/别名，真正的子命令从 {@code args[1]} 开始；
   * 剥掉路由 token 后再交给模块内命令管理器分发。
   */
  private static String[] stripModuleToken(@NotNull String[] args) {
    if (args.length <= 1) {
      return new String[0];
    }
    String[] routed = new String[args.length - 1];
    System.arraycopy(args, 1, routed, 0, routed.length);
    return routed;
  }
}

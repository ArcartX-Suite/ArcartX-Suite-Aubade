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
import xuanmo.arcartxsuite.api.AbstractAXSModule;
import xuanmo.arcartxsuite.api.ModuleCommandHandler;
import xuanmo.arcartxsuite.api.ModuleDescriptor;
import xuanmo.arcartxsuite.api.capability.DatabaseMigratable;

public final class AubadeModule extends AbstractAXSModule implements ModuleCommandHandler {

  private CoreConfig coreConfig;
  private AubadeCore core;
  private CommandManagerImpl commandManager;

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
    mappings.put("arcartx/ui/aubade_main.yml", "arcartx/ui/aubade_main.yml");
    mappings.put("arcartx/ui/aubade_admin.yml", "arcartx/ui/aubade_admin.yml");
    mappings.put("arcartx/ui/aubade_top.yml", "arcartx/ui/aubade_top.yml");
    mappings.put("arcartx/ui/aubade_create.yml", "arcartx/ui/aubade_create.yml");
    mappings.put("arcartx/ui/aubade_invite.yml", "arcartx/ui/aubade_invite.yml");
    mappings.put("arcartx/ui/level_display.yml", "arcartx/ui/level_display.yml");
    mappings.put("arcartx/ui/level_top.yml", "arcartx/ui/level_top.yml");
    mappings.put("arcartx/ui/member_manage.yml", "arcartx/ui/member_manage.yml");
    mappings.put("arcartx/ui/team_settings.yml", "arcartx/ui/team_settings.yml");
    mappings.put("arcartx/ui/challenges_list.yml", "arcartx/ui/challenges_list.yml");
    mappings.put("arcartx/ui/challenge_detail.yml", "arcartx/ui/challenge_detail.yml");
    mappings.put("arcartx/ui/island_bank.yml", "arcartx/ui/island_bank.yml");
    mappings.put("arcartx/ui/warp_board.yml", "arcartx/ui/warp_board.yml");
    mappings.put("arcartx/ui/biome_selector.yml", "arcartx/ui/biome_selector.yml");
    mappings.put("arcartx/ui/border_settings.yml", "arcartx/ui/border_settings.yml");
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
    this.commandManager = new CommandManagerImpl(core);
    core.commandManager(commandManager);
    core.lifecycleManager(new CoreLifecycleManager(core));
    core.getLifecycleManager().onEnable();
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
    if (core != null && core.getLifecycleManager() != null) {
      core.getLifecycleManager().onDisable();
    }
    AubadeCore.set(null);
    core = null;
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
    return commandManager != null ? commandManager.actions() : List.of("help");
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
    if (commandManager == null) {
      return false;
    }
    return commandManager.onCommand(sender, label, normalizeArgs(args));
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
    return commandManager != null ? commandManager.onTabComplete(sender, normalizeArgs(args)) : null;
  }

  private void registerCoreUis() {
    if (core == null || core.getUiManager() == null) {
      return;
    }
    File uiDir = new File(core.dataFolder(), "arcartx/ui");
    String[] uiFiles = {
        "aubade_main.yml", "aubade_admin.yml", "aubade_top.yml", "aubade_create.yml", "aubade_invite.yml",
        "level_display.yml", "level_top.yml",
        "member_manage.yml", "team_settings.yml",
        "challenges_list.yml", "challenge_detail.yml",
        "island_bank.yml", "warp_board.yml",
        "biome_selector.yml", "border_settings.yml"
    };
    for (String fileName : uiFiles) {
      File file = new File(uiDir, fileName);
      if (!file.exists()) {
        core.saveResource("arcartx/ui/" + fileName, false);
      }
      String uiId = fileName.replace(".yml", "");
      core.getUiManager().registerUi(uiId, uiId, file);
    }
  }

  private String[] normalizeArgs(String[] args) {
    if (args.length > 0) {
      String first = args[0];
      if (commandId().equalsIgnoreCase(first) || commandAliases().stream().anyMatch(first::equalsIgnoreCase)) {
        String[] normalized = new String[args.length - 1];
        System.arraycopy(args, 1, normalized, 0, normalized.length);
        return normalized;
      }
    }
    return args;
  }
}

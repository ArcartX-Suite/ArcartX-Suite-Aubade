package xuanmo.aubade.core.lifecycle;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import xuanmo.arcartxsuite.api.aubade.addon.AddonDescriptor;
import xuanmo.arcartxsuite.api.aubade.addon.ExtensionAddon;
import xuanmo.arcartxsuite.api.aubade.addon.FeatureAddon;
import xuanmo.arcartxsuite.api.aubade.addon.GameModeAddon;
import xuanmo.arcartxsuite.api.aubade.addon.SkyAddon;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.storage.StorageManager;

public class AddonLifecycleManager {

  private final AubadeCore core;
  private final StorageManager storageManager;
  private final Logger logger;
  private final Map<String, SkyAddon> addons = new HashMap<>();
  private final Map<String, GameModeAddon> gameModes = new HashMap<>();
  private final Map<String, FeatureAddon> features = new HashMap<>();
  private final Map<String, ExtensionAddon> extensions = new HashMap<>();

  public AddonLifecycleManager(AubadeCore core, StorageManager storageManager) {
    this.core = core;
    this.storageManager = storageManager;
    this.logger = core.getLogger();
  }

  public void loadAddons() {
    File addonsDir = new File(core.getDataFolder(), "addons");
    if (!addonsDir.exists()) {
      addonsDir.mkdirs();
      return;
    }
  }

  public void discoverGameModeAddons(ClassLoader cl) {
    if (cl == null) {
      logger.warning("[组件] 游戏模式发现失败: classLoader 为空");
      return;
    }
    for (GameModeAddon addon : ServiceLoader.load(GameModeAddon.class, cl)) {
      registerAddon(addon);
    }
  }

  public void registerAddon(SkyAddon addon) {
    AddonDescriptor descriptor = addon.descriptor();
    addons.put(descriptor.id(), addon);
    if (addon instanceof GameModeAddon gm) {
      gameModes.put(gm.getGameModeId(), gm);
    } else if (addon instanceof FeatureAddon feat) {
      features.put(feat.getFeatureId(), feat);
    } else if (addon instanceof ExtensionAddon ext) {
      extensions.put(ext.getExtensionId(), ext);
    }
  }

  public void enableAddons() {
    List<SkyAddon> ordered = DependencyResolver.resolve(new ArrayList<>(addons.values()));
    for (SkyAddon addon : ordered) {
      try {
        addon.onLoad();
        addon.onEnable();
        if (addon instanceof GameModeAddon gameModeAddon) {
          gameModeAddon.registerWorlds(core.getWorldManager());
          gameModeAddon.registerCommands(core.getCommandManager());
        }
        logger.info("[component] enabled: " + addon.descriptor().id());
      } catch (Exception e) {
        logger.severe("[组件] 启用失败: " + addon.descriptor().id() + " — " + e.getMessage());
      }
    }
  }

  public void disableAddons() {
    List<SkyAddon> ordered = DependencyResolver.resolve(new ArrayList<>(addons.values()));
    Collections.reverse(ordered);
    for (SkyAddon addon : ordered) {
      try {
        addon.onDisable();
      } catch (Exception e) {
        logger.severe("[组件] 禁用异常: " + addon.descriptor().id() + " — " + e.getMessage());
      }
    }
    addons.clear();
    gameModes.clear();
    features.clear();
    extensions.clear();
  }

  public void reloadAddons() {
    for (SkyAddon addon : addons.values()) {
      try {
        addon.onReload();
      } catch (Exception e) {
        logger.severe("[组件] 重载异常: " + addon.descriptor().id() + " — " + e.getMessage());
      }
    }
  }

  public SkyAddon getAddon(String id) {
    return addons.get(id);
  }

  public GameModeAddon getGameMode(String id) {
    return gameModes.get(id);
  }

  public Collection<GameModeAddon> getGameModes() {
    return Collections.unmodifiableCollection(gameModes.values());
  }

  public Collection<FeatureAddon> getFeatures() {
    return Collections.unmodifiableCollection(features.values());
  }

  public ExtensionAddon getExtension(String id) {
    return extensions.get(id);
  }

  public Collection<ExtensionAddon> getExtensions() {
    return Collections.unmodifiableCollection(extensions.values());
  }
}


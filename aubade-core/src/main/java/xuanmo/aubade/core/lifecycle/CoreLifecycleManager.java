package xuanmo.aubade.core.lifecycle;

import java.io.File;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.config.CoreConfig;
import xuanmo.aubade.core.features.bank.BankAddon;
import xuanmo.aubade.core.features.biomes.BiomesAddon;
import xuanmo.aubade.core.features.blueprint.BlueprintGeneratorAddon;
import xuanmo.aubade.core.features.border.BorderAddon;
import xuanmo.aubade.core.features.cauldronwitchery.CauldronWitcheryAddon;
import xuanmo.aubade.core.features.challenges.ChallengesAddon;
import xuanmo.aubade.core.features.chat.ChatAddon;
import xuanmo.aubade.core.features.checkmeout.CheckMeOutAddon;
import xuanmo.aubade.core.features.controlpanel.ControlPanelAddon;
import xuanmo.aubade.core.features.dimensionaltrees.DimensionalTreesAddon;
import xuanmo.aubade.core.features.extramobs.ExtraMobsAddon;
import xuanmo.aubade.core.features.farmersdance.FarmersDanceAddon;
import xuanmo.aubade.core.features.greenhouses.GreenhousesAddon;
import xuanmo.aubade.core.features.invswitcher.InvSwitcherAddon;
import xuanmo.aubade.core.features.islandfly.IslandFlyAddon;
import xuanmo.aubade.core.features.likes.LikesAddon;
import xuanmo.aubade.core.features.level.LevelAddon;
import xuanmo.aubade.core.features.limits.LimitsAddon;
import xuanmo.aubade.core.features.magicgen.MagicGenAddon;
import xuanmo.aubade.core.features.teams.TeamsAddon;
import xuanmo.aubade.core.features.topblock.TopBlockAddon;
import xuanmo.aubade.core.features.twerkingfortrees.TwerkingForTreesAddon;
import xuanmo.aubade.core.features.upgrades.UpgradesAddon;
import xuanmo.aubade.core.features.visit.VisitAddon;
import xuanmo.aubade.core.features.voidportals.VoidPortalsAddon;
import xuanmo.aubade.core.features.warps.WarpsAddon;
import xuanmo.aubade.game.skyblock.SkyBlockAddon;
import xuanmo.aubade.core.island.IslandGrid;
import xuanmo.aubade.core.island.IslandManagerImpl;
import xuanmo.aubade.core.island.IslandProtectionManager;
import xuanmo.aubade.core.player.PlayerManagerImpl;
import xuanmo.aubade.core.storage.JdbcIslandRepository;
import xuanmo.aubade.core.storage.JdbcPlayerRepository;
import xuanmo.aubade.core.storage.StorageManager;
import xuanmo.aubade.core.ui.UiManager;
import xuanmo.aubade.core.world.WorldManagerImpl;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;

public class CoreLifecycleManager {

  private final AubadeCore core;
  private CoreConfig coreConfig;
  private StorageManager storageManager;
  private AddonLifecycleManager addonLifecycleManager;
  private IslandManagerImpl islandManager;
  private PlayerManagerImpl playerManager;
  private WorldManagerImpl worldManager;
  private IslandProtectionManager protectionManager;
  private UiManager uiManager;

  public CoreLifecycleManager(AubadeCore core) {
    this.core = core;
  }

  public void onEnable() {
    if (core.getCoreConfig() == null) {
      this.coreConfig = new CoreConfig(new File(core.dataFolder(), "config.yml"), core.logger());
      this.coreConfig.load();
      core.coreConfig(coreConfig);
    } else {
      this.coreConfig = core.getCoreConfig();
    }

    this.storageManager = new StorageManager(core.dataFolder(), coreConfig.getStorageDescriptor(), core.logger());
    storageManager.init();
    core.storageManager(storageManager);

    JdbcIslandRepository islandRepo = new JdbcIslandRepository(storageManager.getDataSource());
    IslandGrid grid = new IslandGrid(coreConfig.getRaw().getInt("world.island-spacing", 200));
    this.islandManager = new IslandManagerImpl(core, grid, islandRepo);
    core.islandManager(islandManager);

    JdbcPlayerRepository playerRepo = new JdbcPlayerRepository(storageManager.getDataSource());
    this.playerManager = new PlayerManagerImpl(playerRepo);
    core.playerManager(playerManager);

    this.worldManager = new WorldManagerImpl();
    core.worldManager(worldManager);

    this.protectionManager = new IslandProtectionManager(islandManager);
    Bukkit.getPluginManager().registerEvents(protectionManager, core.plugin());

    this.uiManager = new UiManager(core);
    core.uiManager(uiManager);
    registerCoreUis();

    this.addonLifecycleManager = new AddonLifecycleManager(core, storageManager);
    core.addonLifecycleManager(addonLifecycleManager);
    addonLifecycleManager.registerAddon(new LevelAddon(core));
    addonLifecycleManager.registerAddon(new TeamsAddon(core));
    addonLifecycleManager.registerAddon(new ChallengesAddon(core));
    addonLifecycleManager.registerAddon(new BankAddon(core));
    addonLifecycleManager.registerAddon(new WarpsAddon(core));
    addonLifecycleManager.registerAddon(new BiomesAddon(core));
    addonLifecycleManager.registerAddon(new BorderAddon(core));
    addonLifecycleManager.registerAddon(new IslandFlyAddon(core));
    addonLifecycleManager.registerAddon(new ChatAddon(core));
    addonLifecycleManager.registerAddon(new VisitAddon(core));
    addonLifecycleManager.registerAddon(new LikesAddon(core));
    addonLifecycleManager.registerAddon(new UpgradesAddon(core));
    addonLifecycleManager.registerAddon(new LimitsAddon(core));
    addonLifecycleManager.registerAddon(new InvSwitcherAddon(core));
    addonLifecycleManager.registerAddon(new VoidPortalsAddon(core));
    addonLifecycleManager.registerAddon(new ControlPanelAddon(core));
    addonLifecycleManager.registerAddon(new MagicGenAddon(core));
    addonLifecycleManager.registerAddon(new DimensionalTreesAddon(core));
    addonLifecycleManager.registerAddon(new ExtraMobsAddon(core));
    addonLifecycleManager.registerAddon(new CheckMeOutAddon(core));
    addonLifecycleManager.registerAddon(new TopBlockAddon(core));
    addonLifecycleManager.registerAddon(new FarmersDanceAddon(core));
    addonLifecycleManager.registerAddon(new TwerkingForTreesAddon(core));
    addonLifecycleManager.registerAddon(new CauldronWitcheryAddon(core));
    addonLifecycleManager.registerAddon(new GreenhousesAddon(core));
    addonLifecycleManager.registerAddon(new BlueprintGeneratorAddon(core));
    addonLifecycleManager.registerAddon(new SkyBlockAddon());
    addonLifecycleManager.enableAddons();

  }

  private void registerCoreUis() {
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
      uiManager.registerUi(uiId, uiId, file);
    }
  }

  public void onDisable() {
    if (addonLifecycleManager != null) {
      addonLifecycleManager.disableAddons();
    }
    if (storageManager != null) {
      storageManager.shutdown();
    }
  }

  public void onReload() {
    if (coreConfig != null) {
      coreConfig.load();
    }
    if (addonLifecycleManager != null) {
      addonLifecycleManager.reloadAddons();
    }
  }

  public CoreConfig getCoreConfig() {
    return coreConfig;
  }

  public StorageManager getStorageManager() {
    return storageManager;
  }

  public AddonLifecycleManager getAddonLifecycleManager() {
    return addonLifecycleManager;
  }

  public IslandManagerImpl getIslandManager() {
    return islandManager;
  }

  public PlayerManagerImpl getPlayerManager() {
    return playerManager;
  }

  public WorldManagerImpl getWorldManager() {
    return worldManager;
  }

  public UiManager getUiManager() {
    return uiManager;
  }
}

package xuanmo.aubade.core.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.config.CoreConfig;
import xuanmo.aubade.core.lifecycle.AddonLifecycleManager;
import xuanmo.aubade.core.lifecycle.CoreLifecycleManager;
import xuanmo.aubade.core.player.PlayerManagerImpl;
import xuanmo.aubade.core.storage.JdbcIslandRepository;
import xuanmo.aubade.core.storage.JdbcPlayerRepository;
import xuanmo.aubade.core.storage.StorageManager;
import xuanmo.aubade.game.skyblock.SkyBlockAddon;
import xuanmo.arcartxsuite.api.aubade.addon.GameModeAddon;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.arcartxsuite.api.aubade.player.SkyPlayer;

class IslandLifecycleMockBukkitTest {

  private ServerMock server;
  private AubadeCore core;
  private TestIslandRepository islandRepository;
  private TestPlayerManager playerManager;
  private IslandManagerImpl islandManager;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    server = MockBukkit.mock();
    JavaPlugin plugin = MockBukkit.createMockPlugin();
    new WorldMock(Material.BEDROCK, 3);
    core = new AubadeCore(plugin, tempDir.toFile(), plugin.getClass().getClassLoader(), null);
    CoreConfig config = new CoreConfig(tempDir.resolve("config.yml").toFile(), plugin.getLogger());
    config.load();
    core.coreConfig(config);

    islandRepository = new TestIslandRepository();
    playerManager = new TestPlayerManager();
    TestAddonLifecycleManager addonManager = new TestAddonLifecycleManager(core, tempDir, config);
    addonManager.registerAddon(new SkyBlockAddon());
    core.lifecycleManager(new TestLifecycleManager(core, config, addonManager));
    core.playerManager(playerManager);
    islandManager = new IslandManagerImpl(core, new IslandGrid(200), islandRepository);
    core.islandManager(islandManager);
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void createSaveLookupAndDeleteFlowWorks() {
    PlayerMock owner = server.addPlayer("owner");
    IslandFactory factory = new IslandFactory(new IslandGrid(200));
    Island island = factory.create(owner.getUniqueId(), server.getWorlds().get(0), mockGameMode());

    assertNotNull(island);
    assertEquals(owner.getUniqueId(), island.getOwner());
    playerManager.getPlayer(owner).setIslandId(island.getUniqueId());
    islandManager.saveIsland(island);
    assertTrue(islandManager.getIslandByOwner(owner.getUniqueId()).isPresent());
    assertEquals(1, islandManager.getIslandCount());

    assertTrue(islandManager.deleteIsland(island));
    assertTrue(islandRepository.deletedIds.contains(island.getUniqueId()));
    assertTrue(islandManager.getIslandByOwner(owner.getUniqueId()).isEmpty());
    assertEquals(0, islandManager.getIslandCount());
  }

  private static GameModeAddon mockGameMode() {
    InvocationHandler worldSettingsHandler =
        (proxy, method, args) -> switch (method.getName()) {
          case "getDefaultProtectionRange" -> 50;
          case "getMaxIslandSize" -> 200;
          default -> method.getReturnType().isPrimitive() ? 0 : null;
        };
    InvocationHandler gameModeHandler =
        (proxy, method, args) -> switch (method.getName()) {
          case "getWorldSettings" ->
              Proxy.newProxyInstance(
                  IslandLifecycleMockBukkitTest.class.getClassLoader(),
                  new Class<?>[] {method.getReturnType()},
                  worldSettingsHandler);
          case "getId" -> "skyblock";
          default -> method.getReturnType().isPrimitive() ? 0 : null;
        };
    return (GameModeAddon)
        Proxy.newProxyInstance(
            IslandLifecycleMockBukkitTest.class.getClassLoader(),
            new Class<?>[] {GameModeAddon.class},
            gameModeHandler);
  }

  private static DataSource dummyDataSource() {
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        throw new SQLFeatureNotSupportedException("dummy");
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException("dummy");
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException("dummy");
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return false;
      }

      @Override
      public java.io.PrintWriter getLogWriter() {
        return null;
      }

      @Override
      public void setLogWriter(java.io.PrintWriter out) {
      }

      @Override
      public void setLoginTimeout(int seconds) {
      }

      @Override
      public int getLoginTimeout() {
        return 0;
      }

      @Override
      public Logger getParentLogger() {
        return Logger.getLogger("Aubade");
      }
    };
  }

  private static final class TestLifecycleManager extends CoreLifecycleManager {
    private final CoreConfig config;
    private final AddonLifecycleManager addonManager;

    private TestLifecycleManager(AubadeCore core, CoreConfig config, AddonLifecycleManager addonManager) {
      super(core);
      this.config = config;
      this.addonManager = addonManager;
    }

    @Override
    public CoreConfig getCoreConfig() {
      return config;
    }

    @Override
    public AddonLifecycleManager getAddonLifecycleManager() {
      return addonManager;
    }
  }

  private static final class TestAddonLifecycleManager extends AddonLifecycleManager {
    private TestAddonLifecycleManager(AubadeCore core, Path tempDir, CoreConfig config) {
      super(core, new StorageManager(tempDir.toFile(), config.getStorageDescriptor(), core.getLogger()));
    }
  }

  private static final class TestPlayerManager extends PlayerManagerImpl {
    private final Map<UUID, SkyPlayer> players = new HashMap<>();

    private TestPlayerManager() {
      super(new JdbcPlayerRepository(dummyDataSource()));
    }

    @Override
    public SkyPlayer getPlayer(org.bukkit.entity.Player player) {
      return players.computeIfAbsent(player.getUniqueId(), SkyPlayer::new);
    }

    @Override
    public Optional<SkyPlayer> getPlayer(UUID uuid) {
      return Optional.ofNullable(players.get(uuid));
    }

    @Override
    public void savePlayer(SkyPlayer player) {
      players.put(player.getUuid(), player);
    }

    @Override
    public void unloadPlayer(UUID uuid) {
    }
  }

  private static final class TestIslandRepository extends JdbcIslandRepository {
    private final Map<UUID, Island> islands = new HashMap<>();
    private final java.util.Set<UUID> deletedIds = new java.util.HashSet<>();

    private TestIslandRepository() {
      super(dummyDataSource());
    }

    @Override
    public void save(Island island) {
      islands.put(island.getUniqueId(), island);
    }

    @Override
    public void delete(Island island) {
      islands.remove(island.getUniqueId());
      deletedIds.add(island.getUniqueId());
    }

    @Override
    public Optional<Island> findById(UUID id) {
      return Optional.ofNullable(islands.get(id));
    }

    @Override
    public Optional<Island> findByOwner(UUID owner) {
      return islands.values().stream().filter(island -> owner.equals(island.getOwner())).findFirst();
    }
  }
}

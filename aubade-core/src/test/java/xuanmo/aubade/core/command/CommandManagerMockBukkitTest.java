package xuanmo.aubade.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.config.CoreConfig;

class CommandManagerMockBukkitTest {

  private ServerMock server;
  private CommandManagerImpl commandManager;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    server = MockBukkit.mock();
    JavaPlugin plugin = MockBukkit.createMockPlugin();
    AubadeCore core = new AubadeCore(plugin, tempDir.toFile(), plugin.getClass().getClassLoader(), null);
    CoreConfig config = new CoreConfig(tempDir.resolve("config.yml").toFile(), plugin.getLogger());
    config.load();
    core.coreConfig(config);
    commandManager = new CommandManagerImpl(core);
    core.commandManager(commandManager);
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void islandHelpPrintsHelpText() {
    PlayerMock player = server.addPlayer();

    assertTrue(commandManager.onCommand(player, "island", new String[]{"help"}));
    assertEquals("§6========== §eAubade 岛屿帮助 §6==========", player.nextMessage());
  }

  @Test
  void unknownIslandSubcommandShowsError() {
    PlayerMock player = server.addPlayer();

    assertTrue(commandManager.onCommand(player, "island", new String[]{"not-a-command"}));
    assertTrue(player.nextMessage().contains("未知子命令"));
  }

  @Test
  void adminHelpPrintsHelpTextForAuthorizedPlayer() {
    PlayerMock player = server.addPlayer();
    player.setOp(true);

    assertTrue(commandManager.onCommand(player, "isadmin", new String[]{"isadmin"}));
    assertEquals("§6========== §eAubade 管理帮助 §6==========", player.nextMessage());
  }

  @Test
  void adminReloadDeniedWithoutPermission() {
    PlayerMock player = server.addPlayer();
    player.setOp(false);

    assertTrue(commandManager.onCommand(player, "isadmin", new String[]{"isadmin", "reload"}));
    assertEquals("§c你没有权限使用此命令。", player.nextMessage());
  }
}

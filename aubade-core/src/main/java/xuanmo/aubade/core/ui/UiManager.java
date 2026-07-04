package xuanmo.aubade.core.ui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.arcartxsuite.api.bridge.PacketBridgeAPI;

public class UiManager {

  private final Logger logger;
  private final @Nullable PacketBridgeAPI packetBridge;
  private final Map<String, String> runtimeIds = new HashMap<>();

  public UiManager(AubadeCore core) {
    this.logger = core.getLogger();
    this.packetBridge = core.packetBridge();
    if (isUiAvailable()) {
      logger.info("[UI] 已接入 ArcartX 客户端 UI 桥接。");
    } else {
      logger.warning("[UI] ArcartX UI 桥接不可用，UI 功能已降级。");
    }
  }

  public boolean isUiAvailable() {
    return packetBridge != null && packetBridge.isAvailable();
  }

  public boolean registerUi(String name, String uiId, File uiFile) {
    if (!isUiAvailable()) {
      return false;
    }
    try {
      PacketBridgeAPI.UiRegistrationResult result = packetBridge.registerOrReloadUi(uiId, uiFile);
      runtimeIds.put(uiId, result.runtimeUiId());
      return result.success();
    } catch (Exception e) {
      logger.warning("[UI] 注册 UI 失败 [" + uiId + "]: " + e.getMessage());
      return false;
    }
  }

  public boolean openUi(Player player, String uiId) {
    if (!isUiAvailable()) {
      return false;
    }
    try {
      String runtimeId = runtimeIds.getOrDefault(uiId, uiId);
      return packetBridge.openUi(player, runtimeId);
    } catch (Exception e) {
      logger.warning("[UI] 打开 UI 失败 [" + uiId + "]: " + e.getMessage());
      return false;
    }
  }

  public boolean sendPacket(Player player, String uiId, String handler, Object payload) {
    if (!isUiAvailable()) {
      return false;
    }
    try {
      String runtimeId = runtimeIds.getOrDefault(uiId, uiId);
      return packetBridge.sendPacket(player, runtimeId, handler, payload);
    } catch (Exception e) {
      logger.warning("[UI] 发送 Packet 失败 [" + uiId + "]: " + e.getMessage());
      return false;
    }
  }
}

package xuanmo.aubade.core.features.checkmeout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import xuanmo.arcartxsuite.api.aubade.addon.AddonDescriptor;
import xuanmo.arcartxsuite.api.aubade.island.Island;
import xuanmo.aubade.core.AubadeCore;
import xuanmo.aubade.core.command.IslandCheckMeOutCommand;
import xuanmo.aubade.core.features.AbstractExtensionAddon;

/**
 * 岛屿审核投票扩展组件。
 * 玩家提交岛屿供审核，其他玩家可投票点赞。
 */
public class CheckMeOutAddon extends AbstractExtensionAddon {

  private final Map<UUID, SubmittedIsland> submissions = new ConcurrentHashMap<>();
  private final Map<UUID, java.util.Set<UUID>> votes = new ConcurrentHashMap<>();

  public CheckMeOutAddon(AubadeCore core) {
    super(core, AddonDescriptor.builder("checkmeout")
        .name("岛屿审核")
        .version("1.0.0")
        .mainClass(CheckMeOutAddon.class.getName())
        .build());
  }

  @Override
  public String getExtensionId() {
    return "checkmeout";
  }

  @Override
  public String getFriendlyName() {
    return "岛屿审核";
  }

  @Override
  public void onEnable() {
    super.onEnable();
    try {
      getCommandManager().registerSubCommand("island", new IslandCheckMeOutCommand(core));
    } catch (Exception e) {
      core.getLogger().warning("[CheckMeOut] 注册命令失败: " + e.getMessage());
    }
    core.getLogger().info("[CheckMeOut] 岛屿审核扩展已启用。");
  }

  @Override
  public void onDisable() {
    super.onDisable();
    submissions.clear();
    votes.clear();
    core.getLogger().info("[CheckMeOut] 岛屿审核扩展已禁用。");
  }

  /**
   * 提交岛屿。
   */
  public boolean submit(UUID islandId, String submitter) {
    if (submissions.containsKey(islandId)) {
      return false; // 已提交
    }
    submissions.put(islandId, new SubmittedIsland(islandId, submitter, System.currentTimeMillis()));
    votes.put(islandId, ConcurrentHashMap.newKeySet());
    return true;
  }

  /**
   * 投票。
   */
  public boolean vote(UUID islandId, UUID voter) {
    java.util.Set<UUID> set = votes.get(islandId);
    if (set == null) {
      return false; // 未提交的岛屿
    }
    return set.add(voter);
  }

  /**
   * 获取岛屿票数。
   */
  public int getVotes(UUID islandId) {
    java.util.Set<UUID> set = votes.get(islandId);
    return set != null ? set.size() : 0;
  }

  /**
   * 获取排行榜（按票数降序）。
   */
  public List<SubmittedIsland> getLeaderboard(int topN) {
    List<SubmittedIsland> list = new ArrayList<>(submissions.values());
    list.sort(Comparator.comparingInt((SubmittedIsland s) -> getVotes(s.islandId)).reversed());
    return list.size() > topN ? list.subList(0, topN) : list;
  }

  public List<SubmittedIsland> getAllSubmissions() {
    return new ArrayList<>(submissions.values());
  }

  public Optional<SubmittedIsland> getSubmission(UUID islandId) {
    return Optional.ofNullable(submissions.get(islandId));
  }

  public boolean isSubmitted(UUID islandId) {
    return submissions.containsKey(islandId);
  }

  public boolean hasVoted(UUID islandId, UUID player) {
    java.util.Set<UUID> set = votes.get(islandId);
    return set != null && set.contains(player);
  }

  public record SubmittedIsland(UUID islandId, String submitter, long submitTime) {
  }
}


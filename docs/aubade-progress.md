# ArcartX-Aubade 进度与计划

## 当前定位
ArcartX-Aubade 已完成 AXS 模块化收口，当前主线是继续把各功能 addon 从“有逻辑”补成“可入口、可验证、可交付”的闭环，同时按执行计划推进阶段 2 / 阶段 3 / 阶段 5。

## 已完成（已 push）
| commit | 里程碑 |
|---|---|
| `685e66a` | 动态发现注册游戏模式（ServiceLoader），并加固 `/island create` |
| `1179812` | 退役 orphan 模块 `aubade-api`，契约收口到 `axs-api` |
| `e2aac46` | 模块化打包收口：模块 jar 不再内嵌 `axs-api`，退役 `AubadePlugin` |
| `154343d` | UI 收口到宿主 `packetBridge`，去除反射适配层 |
| `e27b896` | 清理插件语义残留，补齐岛屿保护 listener 事件链，变量命名统一到 `core` |

## 已完成（本地待推）
| 功能 | 入口 / 关键点 | 说明 |
|---|---|---|
| 岛屿空间索引 | `IslandGrid` + `IslandManagerImpl#getIslandAt()` | 用网格/空间索引替代线性遍历，创建/保存/删除时同步索引 |
| 岛屿升级 | `/island upgrade` | `UpgradesAddon` + `UpgradeCommand`；Vault 扣玩家个人余额，升级结果落到岛屿并保存 |
| 岛屿参观 | `/island visit <玩家名>`、`/island leave` | `VisitAddon` + `IslandVisitCommand`；记录返回点，`leave` 可退出参观 |
| 岛屿展示推荐 | `/island checkmeout`、`/island checkmeout list`、`/island checkmeout vote <玩家>` | `CheckMeOutAddon` + `IslandCheckMeOutCommand`；提交自己的岛屿、列出推荐、投票 |
| 岛屿传送点 | `/island warp list`、`/island warp <名称>`、`/island setwarp <名称>`、`/island delwarp <名称>` | `WarpsAddon` 接通既有命名 warp；`warp list` 作为列出入口 |
| 岛屿群系切换 | `/island biome <群系>` | `BiomesAddon` + `IslandBiomeCommand`；按保护范围批量改 biome，并刷新 chunk |

## 进行中 / 待办
| 阶段 / 条目 | 状态 | 优先级 | 说明 |
|---|---|---|---|
| 阶段 2.20 集成测试 | 未完成 | P0 | 计划要求做创建→邀请→踢出→删除的链路验收；当前仓内没有可直接复用的测试基建，`gradlew.bat test` 仅为 `NO-SOURCE` |
| 阶段 3.1~3.6 UI 统一管理与 packetBridge / UI YAML 收口 | 待继续对照计划验收 | P0~P1 | 计划里这一段是阶段 2 之后的下一组里程碑；仓内已有 UI 收口成果，但执行计划仍需逐项对齐确认 |
| 阶段 5.8 岛屿银行 / AXS warehouse 统一体系 | 计划中 / 未实施 | P1 | 目标是让“岛屿仓库 + 岛屿银行”统一由 AXS warehouse 提供存储与经济能力，Aubade 侧只做接入；当前 `UpgradesAddon` 已明确改为扣玩家个人 Vault 余额，岛屿银行暂不作为升级资金来源 |
| 阶段 5.1~5.7 其余玩法联动、跨服同步、全模块联调 | 待后续推进 | P0~P1 | 包括 `InvSwitcher / IslandFly / Limits / Likes / Visit / VoidPortals / Greenhouses` 等后续玩法整合，以及 Redis / `IslandSyncPacket` / 全模块联调 |

## 已知约束 / 部署提醒
- 当前代码以 AXS 模块形态组织，部署时需要重新构建模块 jar 再替换服务端。
- 现阶段 `gradlew.bat test` 没有可执行测试用例，测试基建仍需后续补齐。
- `UpgradesAddon` 的升级资金来源已明确是**玩家个人 Vault 余额**，不是 `Island.getBankBalance()`。
- 新增入口命令都沿用现有 `CompositeCommand` / `registerSubCommand("island", ...)` 范式，便于继续扩展。
- 当前文档反映的是“已 push 的基础地基 + 本地未推功能闭环 + 计划中待办”，不是单纯的代码实现清单。

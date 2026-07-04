# Aubade → AXS 模块系列 重构设计 (v1)

> 现有系列命名：**Ensemble**（合奏，指 ArcartXSuite/modules 下现有 26 个模块）
> Aubade 系列：**独立仓库 `ArcartX-Aubade`**，各模块 jar 通过 axs-api 中的共享契约互通。
>
> 注：`aubade-api` 为早期规划中的仓内契约名；当前共享契约已由 `ArcartXSuite/axs-api` 提供。

## 0. 关键约束（已确认）
- 每个 AXS 模块 jar 有独立 `ModuleClassLoader`，父加载器 = AXS 宿主。跨模块 jar **只能共享 axs-api 里的类型**。
- 因此 Aubade 的跨模块契约（IslandService / IslandView / PlayerService / 事件等）必须放进 **`ArcartXSuite/axs-api`** 的新包 `xuanmo.arcartxsuite.api.aubade.*`。
- 加进 axs-api 后必须**重建并部署新的 ArcartXSuite 运行时 jar**（否则模块运行期找不到契约类）。
- 存储：用户已选 **AXS 统一存储**（`AbstractModuleRepository` + `StorageDescriptor` + `DatabaseMigratable`，接入 `/axs migrate`）。宿主提供 HikariCP / sqlite / mysql 驱动 → 模块 **不再 shade Hikari**。
- 命令：走 `ModuleCommandHandler` → `/axs aubade ...`（如需 `/island` 独立命令，需在宿主 plugin.yml 声明后用 commandBindings 绑定）。
- UI：用注入的 `packetBridge` + `registerModuleUi(...)`，替换旧的 `ArcartXUiBridge` 反射（原崩溃根因）。

## 1. AXS 模块契约要点（已核对 axs-api 源码）
- 模块主类 `extends AbstractAXSModule`，实现 `descriptor()` / `loadConfiguration(File)` / `startService()` / `stopService()`。
- 基类注入字段：`plugin`(宿主 JavaPlugin)、`logger`、`dataFolder`(data/<id>/)、`packetBridge`、`currencyManager`、`placeholderResolver`、`expansionRegistry` 等。
- 声明式钩子：`configFileName()`(自动导出到 data/<id>/config.yml)、`messagesFileName()`、`uiResourceMappings()`、`createListeners()`、`commandBindings()`、`createPlaceholderExpansion()`。
- `registerCapability(Class,impl)` / `getCapability(Class)` 跨模块通信；`ModuleCommandHandler` 提供 `/axs <id>` 子命令。
- `module.yml`：`id / name / version / main / api-version / depends / softdepends / external-depends / external-softdepends`。
- 构建：`id("java")`，`compileOnly` axs-api(+axs-core)+spigot/paper-api+annotations+HikariCP+placeholderapi；`tasks.jar { archiveBaseName=... }`。开源仓无需 AXS 的 `ProtectYamlResourcesTask`（明文 yml 即可）。

## 2. 现状（ArcartX-Aubade 已有 ~120 文件）
- `aubade-api`：早期规划中的仓内契约接口；当前已迁到 `ArcartXSuite/axs-api`。
- `aubade-core`：`AubadePlugin extends JavaPlugin` + `CoreLifecycleManager` + 内部 addon 体系（`AddonLifecycleManager`/`AbstractFeatureAddon`/`DependencyResolver`）+ 18 命令 + island/player/world + storage(JDBC) + UI(反射桥) + 26 个功能 addon。
- `aubade-game-skyblock`：仅 2 文件（薄）。
- 强耦合点：几乎所有 addon/manager 构造签名吃 `AubadePlugin`（`new LevelAddon(plugin)`），且大量 `AubadePlugin.getInstance()`。

## 3. 目标结构（end-state 模块拆分，jar 粒度可调）
> 不做 26 个微 jar；按内聚度合并为 ~11 个：

| jar | 内容 | 依赖 |
|-----|------|------|
| **aubade-core**（必需） | island/player/world/storage/protection/blueprint/核心命令(`/axs aubade`)/核心 UI；注册 IslandService/PlayerService/WorldService 能力 | axs-api |
| aubade-level | 岛屿等级计算 + 排行 + level UI | core |
| aubade-challenges | 挑战系统 | core |
| aubade-teams | 队伍/成员/角色 + member UI | core |
| aubade-bank | 岛屿银行经济 | core |
| aubade-warps | 玩家传送板 | core |
| aubade-biomes | 生物群系选择 | core |
| aubade-border | 岛屿边界 | core |
| aubade-upgrades | upgrades + limits | core |
| aubade-extras | 小型 QoL addon 合集（islandfly/chat/visit/likes/invswitcher/voidportals/controlpanel/magicgen/dimensionaltrees/extramobs/checkmeout/topblock/farmersdance/twerkingfortrees/cauldronwitchery/greenhouses/blueprintgen） | core |
| aubade-game-skyblock | 空岛玩法（世界设置/生成器/默认蓝图） | core |

### 共享契约（新增到 `ArcartXSuite/axs-api` → 包 `xuanmo.arcartxsuite.api.aubade`）
- `IslandService`（create/delete/getByPlayer/getByLocation/save…）+ `IslandView`（只读岛屿数据）
- `PlayerService` + `PlayerView`
- `WorldService`
- 跨 jar 事件（如 `IslandLevelChangeEvent`）如需被功能模块监听，事件类也放这里。
- 契约面**随功能模块抽取逐步增补**，避免一次性过度设计。

## 4. `AubadeCore` 上下文抽象（解耦 AubadePlugin）
新增 `xuanmo.aubade.core.AubadeCore`：持有 `JavaPlugin plugin`、`Logger`、`dataFolder`、各 manager、`packetBridge`。
- 替换 addon/manager 构造里的 `AubadePlugin` → `AubadeCore`（或直接 `JavaPlugin`+服务定位）。
- `AubadeModule.startService()` 构造 `AubadeCore(context)` 并驱动现有 `CoreLifecycleManager` 逻辑。
- 保留 `getInstance()` 语义（静态持有 AubadeCore）以最小化 addon 改动面。

## 5. 分阶段执行（每阶段可构建、可实机验证）
- **P1 基础可加载单模块**：aubade-core 转为 `AubadeModule extends AbstractAXSModule implements ModuleCommandHandler`；module.yml 取代 plugin.yml；AubadeCore 抽象；storage 转 AbstractModuleRepository；UI 走 packetBridge；`/axs aubade` 命令。**先在功能全开/部分关的情况下加载成功**（修掉原 UI 崩溃）。
  - P1a：build+module.yml+AubadeModule 骨架+AubadeCore，编译通过、部署、**日志确认 enable**（island/storage/命令，UI 降级或接通）。
  - P1b：接回内部 addon 体系（26 功能仍内置），整体加载。
  - P1c：storage 全面转 AXS 统一存储 + DatabaseMigratable。
  - P1d：核心 UI 走 packetBridge 实机确认（面板需真人客户端，先确认注册不报错）。
- **P2 拆分模块系列**：把契约上移 axs-api（重建部署宿主）；按 §3 逐个抽出功能模块 jar，各自 compileOnly axs-api，注册/消费能力；逐个构建部署验证。
- **P3 开源打磨**：README/文档/CI（`.github/workflows`）、config 开关、版本号占位符修复。

## 6. 工作流（隧道 + 本地镜像）
- 本地镜像：`C:\Users\Administrator\work\ArcartX-Aubade`（作者用，快速编辑）；参考：`work\ref\axsref`（axs-api 源 + afkreward/battlepass/announcer/warehouse）。
- 编辑在镜像 → 批量 upload 回 `D:\IDEA\project\ArcartX-Aubade` → 远程 `gradlew` 构建 → 部署 `plugins/ArcartXSuite/modules/` → 读日志。
- 仅改本地，不 push GitHub（由作者提交）。

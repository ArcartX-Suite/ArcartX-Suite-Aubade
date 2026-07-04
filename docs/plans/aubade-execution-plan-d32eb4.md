# ArcartX-Aubade 项目执行计划

**项目**：ArcartX-Aubade — 岛屿游戏生态平台  
**蓝图文件**：`C:\Users\杨骋达\.windsurf\plans\aubade-0*-d32eb4.md`（7 个分模块蓝图）  
**当前阶段**：📋 策划完成，待进入阶段 1（基础框架）  

---

## 执行原则

1. **蓝图先行**：每个任务开始前，先查阅对应蓝图文件获取设计决策
2. **渐进交付**：每个阶段产出可编译/可运行的中间产物
3. **测试驱动**：数据库层、命令路由、配置诊断等关键模块先写测试
4. **中文优先**：所有注释、消息、配置注释使用中文
5. **版本锁定**：`compileOnly` 固定 Paper 1.20.1，运行时反射适配高版本

---

## 阶段 1：基础框架（目标：4-6 周）

### 1.1 Gradle 多模块工程搭建

**参考蓝图**：[→ 01 项目定位与架构总览](aubade-01-overview-d32eb4.md#3-工程结构)  
**产出**：可编译的工程骨架

| 序号 | 任务 | 说明 | 优先级 | 依赖 |
|---|---|---|---|---|
| 1.1.1 | 创建根项目目录 `ArcartX-Aubade/` | 初始化 Git 仓库 | P0 | 无 |
| 1.1.2 | 编写 `settings.gradle.kts` | 包含 `aubade-api`, `aubade-core` 及所有游戏/功能/扩展模块 | P0 | 1.1.1 |
| 1.1.3 | 编写根 `build.gradle.kts` | Kotlin DSL，配置 shadow 插件、仓库、版本号 | P0 | 1.1.2 |
| 1.1.4 | 创建 `buildSrc/` 自定义任务 | `ProtectYamlResourcesTask.kt`（YAML 资源保护） | P1 | 1.1.2 |
| 1.1.5 | 创建 `aubade-api` 模块 | 空包结构，配置 `build.gradle.kts`（只发布到 Maven，不 shadow） | P0 | 1.1.2 |
| 1.1.6 | 创建 `aubade-core` 模块 | 配置 `build.gradle.kts`（shadow + 软依赖 ArcartX/AXS/Vault/PAPI/Redis） | P0 | 1.1.2 |
| 1.1.7 | 创建各组件空模块 | `aubade-game-skyblock` 到 `aubade-ext-chat`，每个模块 `build.gradle.kts` 只依赖 `aubade-api` 和 `paper-api` | P1 | 1.1.5 |
| 1.1.8 | 根项目 `./gradlew build` 验证 | 所有模块编译通过 | P0 | 1.1.7 |

### 1.2 aubade-api 接口设计

**参考蓝图**：[→ 01 项目定位与架构总览](aubade-01-overview-d32eb4.md#3-工程结构) / [→ 03 核心数据模型](aubade-03-datamodel-d32eb4.md#1-核心数据对象)  
**产出**：`aubade-api` 模块所有接口定义，通过编译

| 序号 | 任务 | 说明 | 优先级 | 依赖 |
|---|---|---|---|---|
| 1.2.1 | `addon/` 包：SkyAddon, GameModeAddon, FeatureAddon, AddonDescriptor, AddonClassLoader | 组件生命周期接口 | P0 | 1.1.5 |
| 1.2.2 | `island/` 包：Island, IslandManager, IslandPermission, Role, IslandMember, CoopPlayer | 岛屿数据与权限模型 | P0 | 1.2.1 |
| 1.2.3 | `player/` 包：SkyPlayer, PlayerManager | 玩家数据模型 | P0 | 1.2.2 |
| 1.2.4 | `world/` 包：WorldSettings, GameWorld, ChunkGeneratorFactory | 世界配置接口 | P0 | 1.2.1 |
| 1.2.5 | `storage/` 包：Repository, StorageDescriptor, DataObject | 持久层接口 | P0 | 1.2.2 |
| 1.2.6 | `ui/` 包：UiBridge, PacketPayloadBuilder | UI 桥接抽象 | P0 | 1.2.1 |
| 1.2.7 | `event/` 包：IslandEvent 及子事件 | 事件体系 | P0 | 1.2.2 |
| 1.2.8 | `command/` 包：CompositeCommand | 命令基类 | P0 | 1.2.1 |
| 1.2.9 | `permission/` 包：Permission | 权限常量 | P0 | 1.2.2 |
| 1.2.10 | `config/` 包：ConfigComment, ConfigEntry | 配置注解 | P1 | 1.2.1 |
| 1.2.11 | API 模块编译验证 | `./gradlew :aubade-api:build` | P0 | 1.2.10 |

### 1.3 aubade-core 基础骨架

**参考蓝图**：[→ 01 项目定位与架构总览](aubade-01-overview-d32eb4.md#3-工程结构)  
**产出**：插件可加载，命令可用，配置可读写

| 序号 | 任务 | 说明 | 优先级 | 依赖 |
|---|---|---|---|---|
| 1.3.1 | `AubadePlugin.java` 主类 | 继承 JavaPlugin，管理生命周期 | P0 | 1.2.11 |
| 1.3.2 | `lifecycle/CoreLifecycleManager.java` | 核心启动/关闭/重载协调 | P0 | 1.3.1 |
| 1.3.3 | `lifecycle/AddonLifecycleManager.java` | 组件加载、启用、禁用、重载 | P0 | 1.3.2 |
| 1.3.4 | `lifecycle/DependencyResolver.java` | 组件依赖拓扑排序 | P1 | 1.3.3 |
| 1.3.5 | `config/CoreConfig.java` + `config.yml` | 核心配置 POJO | P0 | 1.3.1 |
| 1.3.6 | `config/ConfigDiagnosticEngine.java` | 移植 AXS 配置诊断（见 [→ 07](aubade-07-config-roadmap-d32eb4.md#1-配置诊断系统)） | P1 | 1.3.5 |
| 1.3.7 | `config/SyncPolicy.java`, `ValidationRule.java`, `ValueType.java` | 诊断规则定义 | P1 | 1.3.6 |
| 1.3.8 | `messages.yml` | 消息配置模板 | P0 | 1.3.1 |
| 1.3.9 | `plugin.yml` | 插件描述符（软依赖 ArcartX/AXS/Vault/PAPI） | P0 | 1.3.1 |
| 1.3.10 | `./gradlew :aubade-core:build` 验证 | 核心模块编译通过 | P0 | 1.3.9 |

### 1.4 数据库层

**参考蓝图**：[→ 03 核心数据模型与数据库](aubade-03-datamodel-d32eb4.md#2-数据库设计)  
**产出**：完整表结构，HikariCP 连接池，基础 CRUD

| 序号 | 任务 | 说明 | 优先级 | 依赖 |
|---|---|---|---|---|
| 1.4.1 | `storage/StorageManager.java` | HikariCP 连接池管理（SQLite 默认 + MySQL 可选） | P0 | 1.3.10 |
| 1.4.2 | `storage/JdbcIslandRepository.java` | 岛屿表 CRUD | P0 | 1.4.1 |
| 1.4.3 | `storage/JdbcPlayerRepository.java` | 玩家表 CRUD | P0 | 1.4.1 |
| 1.4.4 | `storage/JdbcWorldRepository.java` | 世界配置表 CRUD | P1 | 1.4.1 |
| 1.4.5 | `storage/MigrationManager.java` | 数据库版本迁移 | P1 | 1.4.1 |
| 1.4.6 | 建表语句测试 | 启动时自动建表，验证表结构 | P0 | 1.4.5 |

### 1.5 命令框架

**参考蓝图**：[→ 06 UI 与命令系统](aubade-06-ui-commands-d32eb4.md#4-命令系统)  
**产出**：`/island help` 可用

| 序号 | 任务 | 说明 | 优先级 | 依赖 |
|---|---|---|---|---|
| 1.5.1 | `command/CommandManager.java` | 命令注册总控 | P0 | 1.3.10 |
| 1.5.2 | `command/CompositeCommandImpl.java` | 复合命令实现 | P0 | 1.5.1 |
| 1.5.3 | `command/DefaultPlayerCommand.java` | `/island` 根命令 + help | P0 | 1.5.2 |
| 1.5.4 | `command/DefaultAdminCommand.java` | `/isadmin` 根命令 + help | P0 | 1.5.2 |

### 1.6 阶段 1 里程碑检查

- [ ] `./gradlew build` 全部模块编译通过
- [ ] Paper 1.20.1 测试服可加载 `aubade-core`
- [ ] `/island help` 返回帮助信息
- [ ] `/isadmin help` 返回管理帮助
- [ ] 数据库自动建表无报错
- [ ] 配置诊断引擎启动无异常

---

## 阶段 2：岛屿核心（目标：4-6 周）

**参考蓝图**：[→ 03 核心数据模型](aubade-03-datamodel-d32eb4.md) / [→ 04 游戏模式设计](aubade-04-gamemodes-d32eb4.md)  
**产出**：玩家可创建岛屿，有保护，可邀请成员

| 序号 | 任务 | 说明 | 优先级 | 依赖 |
|---|---|---|---|---|
| 2.1 | `island/IslandManagerImpl.java` | 创建/删除/查询/缓存 | P0 | 1.4.6 |
| 2.2 | `island/IslandGrid.java` | 网格坐标计算，间距保护 | P0 | 2.1 |
| 2.3 | `island/IslandFactory.java` | 岛屿创建工厂（调用世界生成） | P0 | 2.2 |
| 2.4 | `island/IslandProtectionManager.java` | 保护事件拦截 | P0 | 2.1 |
| 2.5 | `island/IslandCache.java` | 三级缓存（运行时 + Caffeine + DB） | P1 | 2.1 |
| 2.6 | `player/PlayerManagerImpl.java` | 玩家数据管理 | P0 | 1.4.6 |
| 2.7 | `world/WorldManagerImpl.java` + `WorldFactory.java` | 动态世界创建 | P0 | 2.3 |
| 2.8 | `world/SkyBlockChunkGenerator.java` | 经典空岛生成器 | P0 | 2.7 |
| 2.9 | `blueprint/Blueprint.java` + `BlueprintParser.java` | JSON 蓝图解析 | P0 | 2.8 |
| 2.10 | `blueprint/BlueprintPaster.java` | 异步粘贴 | P0 | 2.9 |
| 2.11 | `command/IslandCreateCommand.java` | `/island create [blueprint]` | P0 | 2.10 |
| 2.12 | `command/IslandDeleteCommand.java` | `/island delete [confirm]` | P0 | 2.1 |
| 2.13 | `command/IslandHomeCommand.java` | `/island home` | P0 | 2.1 |
| 2.14 | `command/IslandSetHomeCommand.java` | `/island sethome` | P0 | 2.1 |
| 2.15 | `command/IslandInviteCommand.java` | `/island invite <player>` | P0 | 2.1 |
| 2.16 | `command/IslandAcceptCommand.java` | `/island accept` | P0 | 2.15 |
| 2.17 | `command/IslandKickCommand.java` | `/island kick <player>` | P0 | 2.1 |
| 2.18 | `listener/*` | 保护/移动/交互/实体事件监听 | P0 | 2.4 |
| 2.19 | `skydream-game-skyblock` 模块完整实现 | 第一个游戏模式 | P0 | 2.8 |
| 2.20 | 阶段 2 集成测试 | 创建→邀请→踢出→删除完整流程 | P0 | 2.19 |

---

## 阶段 3：ArcartX UI 集成（目标：3-4 周）

**参考蓝图**：[→ 02 ArcartX/AXS 深度联动](aubade-02-arcartx-integration-d32eb4.md#4-ui-桥接实现) / [→ 06 UI 与命令系统](aubade-06-ui-commands-d32eb4.md)  
**产出**：所有管理操作可通过 ArcartX UI 完成

| 序号 | 任务 | 说明 | 优先级 | 依赖 |
|---|---|---|---|---|
| 3.1 | `ui/UiManager.java` + `UiRegistry.java` | UI 统一管理 | P0 | 2.20 |
| 3.2 | `ui/bridge/ArcartXUiBridge.java` | 反射封装 PacketBridgeAPI | P0 | 3.1 |
| 3.3 | `ui/packet/BasePacketHandler.java` | Packet 处理基类 | P0 | 3.2 |
| 3.4 | `ui/packet/IslandUiPacketHandler.java` | 岛屿面板 Packet 处理 | P0 | 3.3 |
| 3.5 | `ui/packet/AdminUiPacketHandler.java` | 管理面板 Packet 处理 | P1 | 3.3 |
| 3.6 | 核心 UI YAML 文件（5 个） | `aubade_main.yml`, `aubade_admin.yml`, `aubade_top.yml`, `aubade_create.yml`, `aubade_invite.yml` | P0 | 3.4 |
| 3.7 | 降级机制 | 无 ArcartX 时回退到 Bukkit Inventory | P1 | 3.2 |
| 3.8 | `./gradlew build` + UI 功能测试 | 有/无 ArcartX 两种场景 | P0 | 3.7 |

---

## 阶段 4：功能组件（目标：6-8 周）

**参考蓝图**：[→ 05 功能与扩展组件](aubade-05-features-d32eb4.md)  
**产出**：8 个核心功能组件全部可用

| 序号 | 任务 | 组件 | 优先级 | 依赖 |
|---|---|---|---|---|
| 4.1 | 方块扫描算法 + 价值表 | Level | P0 | 3.8 |
| 4.2 | 排行榜缓存 + HUD UI | Level | P0 | 4.1 |
| 4.3 | 多类型挑战定义 + 进度跟踪 | Challenges | P0 | 3.8 |
| 4.4 | 挑战列表/详情 UI | Challenges | P0 | 4.3 |
| 4.5 | 成员管理 UI + 权限矩阵 | Teams | P0 | 3.8 |
| 4.6 | 邀请/踢出/转让完整流程 | Teams | P0 | 4.5 |
| 4.7 | 传送牌创建/删除 + 公共面板 | Warps | P1 | 3.8 |
| 4.8 | 岛屿银行存取 + 利率 | Bank | P1 | 3.8 |
| 4.9 | 生物群系更换 + 选择 UI | Biomes | P1 | 3.8 |
| 4.10 | 世界边界/粒子边界 | Border | P1 | 3.8 |

---

## 阶段 5：扩展组件 + AXS 联动（目标：4-6 周）

**参考蓝图**：[→ 02 ArcartX/AXS 深度联动](aubade-02-arcartx-integration-d32eb4.md) / [→ 05 功能与扩展组件](aubade-05-features-d32eb4.md#3-扩展组件)  
**产出**：扩展组件 + AXS 能力注册 + 跨服同步

| 序号 | 任务 | 说明 | 优先级 | 依赖 |
|---|---|---|---|---|
| 5.1 | 8 个扩展组件开发 | InvSwitcher/IslandFly/Limits/Likes/Upgrades/Visit/VoidPortals/Greenhouses | P1 | 4.10 |
| 5.2 | 10 个轻量扩展组件 | MagicGen/DimensionalTrees/ExtraMobs/... | P2 | 4.10 |
| 5.3 | `api/capability/` 接口定义 | IslandQueryable/Economy/Permission/Shop | P0 | 4.10 |
| 5.4 | AXS 能力注册实现 | 核心启动时注册到 Capability 系统 | P0 | 5.3 |
| 5.5 | ArcartX 深度集成 | 路标/飘字/贴图/按键/聊天卡片 | P1 | 3.8 |
| 5.6 | 跨服同步架构 | Redis + IslandSyncPacket | P1 | 5.4 |
| 5.7 | 全模块联调 | 6 游戏模式 × 8 功能 × 18 扩展 | P0 | 5.6 |
| 5.8 | 岛屿银行 / AXS warehouse 统一体系 | 计划中 / 未实施：岛屿仓库 + 岛屿银行统一由 AXS warehouse 提供存储与经济能力，Aubade 侧接入而非自维护；现阶段 UpgradesAddon 仅扣玩家个人 Vault 余额，岛屿银行暂不作为升级资金来源 | P1 | 5.4 |

---

## 阶段 6：打磨与开源（目标：3-4 周）

**参考蓝图**：[→ 07 配置、诊断与路线图](aubade-07-config-roadmap-d32eb4.md#3-开发路线图)  
**产出**：GitHub Release v1.0.0

| 序号 | 任务 | 说明 | 优先级 |
|---|---|---|---|
| 6.1 | 跨版本测试 | 1.20.1 / 1.20.4 / 1.21.x 兼容性 | P0 |
| 6.2 | 性能优化 | 异步扫描、缓存命中率、连接池调优 | P0 |
| 6.3 | 中文文档 | docs/guide/ + docs/dev/ + docs/architecture/ | P0 |
| 6.4 | GitHub Actions CI | `.github/workflows/build.yml` | P0 |
| 6.5 | Issue/PR 模板 | `.github/ISSUE_TEMPLATE/` + `PULL_REQUEST_TEMPLATE.md` | P1 |
| 6.6 | 开源规范文件 | LICENSE (GPL-3.0) / CONTRIBUTING.md / CODE_OF_CONDUCT.md | P0 |
| 6.7 | README.md | 项目介绍、快速开始、徽章 | P0 |
| 6.8 | Pre-release 测试 | beta/rc 版本社区测试 | P0 |
| 6.9 | v1.0.0 正式发布 | GitHub Release | P0 |

---

## 当前进度跟踪

| 阶段 | 任务数 | 已完成 | 进行中 | 待开始 |
|---|---|---|---|---|
| 阶段 1：基础框架 | 28 | 0 | 0 | 28 |
| 阶段 2：岛屿核心 | 20 | 0 | 0 | 20 |
| 阶段 3：ArcartX UI | 8 | 0 | 0 | 8 |
| 阶段 4：功能组件 | 10 | 0 | 0 | 10 |
| 阶段 5：扩展+联动 | 7 | 0 | 0 | 7 |
| 阶段 6：打磨开源 | 9 | 0 | 0 | 9 |
| **总计** | **82** | **0** | **0** | **82** |

---

## 下一步行动

**当前待命**：等待用户确认开始执行。  
**建议切入点**：阶段 1.1.1（创建根项目目录）→ 阶段 1.1.8（`./gradlew build` 验证）。

当用户说"开始"或指定具体任务时，我将：
1. 查阅对应蓝图文件获取设计决策
2. 按执行计划的优先级逐项实现
3. 每完成一个可编译的里程碑，运行 `./gradlew build` 验证
4. 更新本执行计划的"当前进度跟踪"表

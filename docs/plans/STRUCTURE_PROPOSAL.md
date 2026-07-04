# Aubade 模块系列 — 工程结构方案（待你确认打包形态）

> 你的目标：把 Aubade 彻底做成 AXS 模块系列、功能分类清晰、开源上传 GitHub。
> 动手前有一个 **AXS 类加载机制的硬约束**必须先和你对齐，它直接决定"拆成几个 jar"。
>
> 注：本文里出现的 `aubade-api` 为早期规划/历史表述；当前共享契约已迁移到 `ArcartXSuite/axs-api`。

---

## 1. 关键发现：AXS 模块之间的类是互相隔离的（有据）

反编译 `axs-core` 的 `ModuleRegistry` / `ModuleClassLoader` 确认：

```java
// ModuleRegistry 实例化每个模块时：
new ModuleClassLoader(id, jarUrl, this.plugin.getClass().getClassLoader());
// ModuleClassLoader extends URLClassLoader，只含【自己这一个 jar】+ 父加载器=AXS宿主
```

含义：
- 每个模块 jar 有**独立 ClassLoader**，父加载器是 **AXS 宿主**（能看到 `axs-api`、
  ArcartX、Bukkit），**但看不到别的模块 jar**。
- 所以两个不同模块 jar **无法共享 Aubade 自己定义的类型**（如 `Island`、
  `IslandManager`）。就算各自打包一份 `aubade-api`，类身份也不一致 →
  `getCapability(IslandService.class)` 跨模块会 ClassCastException。
- AXS 官方模块之间之所以能互通，是因为它们只通过 **定义在 `axs-api` 里的能力接口**
  （`SubtitlePlayable`、`MailDispatchable`…，父加载器可见）通信。Aubade 无法往
  `axs-api` 里加自己的领域接口。

**结论**：把 Aubade 拆成"多个各自被 AXS 分别加载、又要共享岛屿等领域对象"的独立 jar，
在 AXS 现有机制下做不到（除非退化成无类型的 JSON/Map 桥接，非常难维护）。

---

## 2. 推荐方案 A：源码多模块 + 发行单个 AXS 模块 jar（强烈推荐）

**源码层**保持清晰的 Gradle 多子项目（利于开源、分类明确、各功能解耦）：

```
ArcartX-Aubade/                     (GitHub 仓库根)
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── buildSrc/                       # 公共构建逻辑（版本、编译选项）
│
├── axs-api/（外部依赖）            # 共享契约已迁入 ArcartXSuite
│   └── xuanmo/arcartxsuite/api/aubade/{island,player,world,storage,feature,event}
│
├── aubade-core/                    # 岛屿/玩家/世界/存储/命令/UI（核心子系统）
│   └── xuanmo/aubade/core/{island,player,world,storage,command,ui,config}
│
├── features/                       # 各功能，独立 Gradle 子项目、独立包、可单测
│   ├── aubade-feature-level/
│   ├── aubade-feature-challenges/
│   ├── aubade-feature-warps/
│   ├── aubade-feature-teams/
│   ├── aubade-feature-bank/
│   ├── aubade-feature-biomes/
│   └── aubade-feature-border/
│
├── games/                          # 各游戏模式子项目
│   ├── aubade-game-skyblock/
│   ├── aubade-game-acidisland/
│   └── aubade-game-oneblock/  ...
│
├── aubade-module/                  # 组装层：AubadeModule + module.yml + 资源
│   └── (依赖上面所有子项目，shadow 合并成一个模块 jar)
│
└── docs/
```

**发行层**：`aubade-module` 用 shadow 把 api/core/features/games 合并成
**一个** `Aubade-x.y.z.jar`，放进 `plugins/ArcartXSuite/modules/`。

- 入口：`AubadeModule extends AbstractAXSModule`（`module.yml` 的 `main`）。
- 每个功能实现统一的内部生命周期接口（如 `AubadeFeature { id/enable/disable/reload }`），
  由 `AubadeModule.startService()` 按 `config.yml` 的 `features.<id>.enabled` 装配。
- 命令走 `ModuleCommandHandler` → `/axs aubade ...`。
- 存储走 AXS 统一存储（`StorageDescriptor` + `AbstractModuleRepository`，
  实现 `DatabaseMigratable` 接 `/axs migrate`）。

优点：**完全符合 AXS 机制且能跑**；源码分类清晰、每个功能独立目录/子项目，
开源友好；功能可用 config 开关；类型安全共享领域对象（同一 jar 内）。
唯一"代价"：对外是一个 jar，而不是十几个分别加载的 jar —— 但这正是 AXS 对
"内聚型插件"的惯用形态。

---

## 3. 备选方案（不推荐，列出供你判断）

- **B. 真·多 jar 分别加载**：只有当各模块之间**不共享 Aubade 领域类型**、仅通过
  `axs-api` 能力接口 + JSON/Map/Bukkit 基础类型通信才可行。岛屿类插件强耦合领域
  对象，会退化成大量无类型桥接，维护成本高、易出错。
- **C. 把 `aubade-api` 单独做成一个 Bukkit 插件**放到 Paper 类加载器，借 Paper 跨
  插件类查找让各模块 jar 都能看到它。可行但依赖 Paper 老式全局类查找、脆弱、
  与 AXS 隔离设计相悖，不建议开源项目采用。

---

## 4. 需要你确认

**是否采用方案 A**（源码多模块、发行单个 AXS 模块 jar）？
确认后我就开始：清空重构 `D:\IDEA\project\ArcartX-Aubade` 为上面的结构，
先对齐 `axs-api` 契约，再搭 `aubade-core` + `aubade-module` 骨架并跑通构建，随后逐个功能填充。

（仍只改本地，不推 GitHub，由你后续上传。）

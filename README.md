# Aubade — 岛屿游戏生态平台

> **Aubade** 是一个面向 Minecraft 服务器的岛屿游戏插件框架，支持多种空岛玩法模式、丰富的功能组件和深度 ArcartX UI 集成。

## 特性

- **多模式支持**：经典空岛、OneBlock、SkyGrid 等多种岛屿玩法
- **26 个内置组件**：7 个核心功能 + 19 个扩展组件，开箱即用
- **自定义蓝图**：JSON/YAML 格式蓝图，轻松定制岛屿初始地形
- **ArcartX 深度集成**：完整 UI 面板、PAPI 占位符、AXS 能力接口
- **高性能设计**：三级缓存、异步操作、HikariCP 数据库连接池
- **灵活配置**：SQLite 默认 + MySQL 可选，支持配置诊断自动修复

## 快速开始

### 环境要求

- **Java 21**
- **Paper 1.20.1+**（运行时反射适配高版本）
- **依赖插件**：ArcartX（必需）、Vault、PlaceholderAPI、ArcartXSuite（可选）

### 安装

1. 从 CI Release 下载 `Aubade-<version>.jar`，或本地执行 `./gradlew build` 后取 `aubade-core/build/libs/Aubade-<version>.jar`
2. 将 JAR 放入 `plugins/ArcartXSuite/modules/`
3. 在 `plugins/ArcartXSuite/config.yml` 的 `modules:` 下加入：

   ```yml
   aubade:
     enabled: true
   ```

4. 重启服务器，或使用 AXS 的模块热加载流程
5. AXS 模块使用 `module.yml`，不需要 `plugin.yml`；也不需要 cloud 的 `qq/apiKey/server-code` 或签名公钥配置

### 玩家命令

```
/island create [蓝图]  — 创建新岛屿
/island home           — 返回岛屿
/island sethome        — 设置岛屿出生点
/island invite <玩家>   — 邀请玩家
/island accept         — 接受邀请
/island kick <玩家>     — 踢出成员
/island leave          — 离开岛屿
/island info           — 查看岛屿信息
/island top            — 排行榜
```

### 管理员命令

```
/isadmin reload        — 重载配置
/isadmin purge         — 清理无效数据
/isadmin info <玩家>    — 查看岛屿详情
/isadmin delete <玩家>   — 强制删除岛屿
```

## 蓝图自定义

在 `plugins/Aubade/blueprints/` 目录放置 YAML 蓝图文件即可自定义岛屿初始地形：

```yaml
id: my_island
name: "我的岛屿"
description: "自定义开局"
icon: DIAMOND_BLOCK
blocks:
  - { x: 0, y: 0, z: 0, material: DIAMOND_BLOCK }
  - { x: 1, y: 0, z: 0, material: GOLD_BLOCK }
```

## 架构

```
Aubade/
├── aubade-core/         # 核心实现（生命周期、数据库、命令、UI）
└── aubade-game-skyblock/ # 经典空岛游戏模式
```

共享契约现由 `ArcartXSuite/axs-api` 提供，Aubade 仓内不再保留独立的 `aubade-api` 模块。

## 构建

```bash
./gradlew build
```

产物位于 `aubade-core/build/libs/Aubade-<version>-all.jar`。

## 开源协议

[GPL-3.0](LICENSE)

## 贡献

欢迎提交 Issue 和 Pull Request！详见 [CONTRIBUTING.md](CONTRIBUTING.md)。

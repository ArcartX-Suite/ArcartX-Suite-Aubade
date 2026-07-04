# API 概览

## 模块结构

```
aubade-core/              # 核心实现
└── aubade-game-skyblock/ # 经典空岛游戏模式
```

共享契约由 `ArcartXSuite/axs-api` 提供，Aubade 仓内不再维护独立 `aubade-api` 模块。

## 获取 IslandManager

```java
// 通过 Bukkit ServicesManager
IslandManager manager = Bukkit.getServicesManager().load(IslandManager.class);
```

## 创建自定义组件

```java
public class MyAddon extends AbstractExtensionAddon {
    @Override
    public String getExtensionId() {
        return "my_addon";
    }

    @Override
    public void onEnable() {
        // 注册监听器、初始化数据等
    }
}
```

## 事件列表

- `IslandCreateEvent` — 岛屿创建
- `IslandDeleteEvent` — 岛屿删除
- `IslandEnterEvent` — 玩家进入岛屿
- `IslandLeaveEvent` — 玩家离开岛屿
- `MemberJoinEvent` / `MemberLeaveEvent` — 成员变动

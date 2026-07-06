# 快速开始

## 环境要求

- Java 21
- Paper 1.20.1+（运行时反射适配高版本）
- ArcartX（必需依赖）
- Vault / PlaceholderAPI / ArcartXSuite（可选）

## 安装步骤

1. 从 [Releases](../../releases) 下载 `Aubade-<version>-all.jar`
2. 将 JAR 放入服务器的 `plugins/ArcartX-Suite/modules` 目录
3. 启动服务器
4. 编辑 `plugins/ArcartX-Suite/data/Aubade/config.yml` 按需调整

## 创建第一个岛屿

作为玩家，进入服务器后执行：

```
/island create
```

这会使用默认蓝图（经典空岛：3×3 平台 + 树 + 箱子）创建你的岛屿。

## 常用命令

| 命令 | 说明 |
|---|---|
| `/island home` | 返回岛屿 |
| `/island invite <玩家>` | 邀请玩家加入 |
| `/island accept` | 接受邀请 |
| `/island kick <玩家>` | 踢出成员 |

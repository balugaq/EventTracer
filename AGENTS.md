# AGENTS.md — AI 协作须知

本文件面向在本仓库工作的 AI 代理（与人类贡献者）。改动前请先读完。

## 项目一句话

Paper 插件：调试事件触发顺序、取消归因、频率监控与游戏内断点单步。最低运行 Paper 1.17（`api-version: 1.17`），Java 17 字节码。

## 环境与构建

- Gradle 9.8.1（wrapper）；依赖版本一律进 `gradle/libs.versions.toml`，不要在 `build.gradle.kts` 里硬编码版本号。
- 主代码 `options.release=17`；测试代码 `release=21`（MockBukkit 4.x 要求 JVM 21+），不要动这两行。
- `./gradlew clean build` 必须全绿（编译 + 测试 + shadowJar）才能交付。
- 不要给 `EventTracePlugin` 类加 `final`（MockBukkit 需要 ByteBuddy 子类化）。

## 关键约束（改坏过一次的地方）

1. **包路径**：`org.bukkit.event.Listener` 与 `org.bukkit.event.EventException` 在 `org.bukkit.event` 包，**不是** `org.bukkit.plugin`。
2. **Paper 1.17 的 `RegisteredListener` 没有公开 `getExecutor()`**（1.21.1 才加）。拿原 executor 必须反射私有字段 `executor`，见 `gate/ExecutorAccess.java`。不要替换成 `getExecutor()`。
3. **测试与主代码的 paper-api 版本不同**：主代码锁定 `1.17.1-R0.1-SNAPSHOT`，测试编译/运行用 `1.21.11-R0.1-SNAPSHOT`（MockBukkit 4.x 的 API 需要 1.21.1+ 的类，且 1.21.1 快照缺 `CustomModelDataComponent`）。两者都在 toml 里，别合并。
4. **ACF 只在 aikar 自有仓库**（`https://repo.aikar.co/content/groups/aikar/`），版本是 `0.5.1-SNAPSHOT`，坐标 `co.aikar:acf-bukkit`。根命令别名用 `@CommandAlias`，嵌套子命令用 `@Subcommand("stats reset")`。
5. **processResources 只 expand `plugin.yml`**。语言/配置文件里有 `{0}` 占位符，走 expand 会被 Gradle 模板引擎吃掉。
6. **shadowJar 不 relocate ACF**（`co.aikar` 命名空间独立）；jar 任务本体禁用（`tasks.jar { enabled = false }`），产物走 shadowJar 且 classifier 为空。

## 架构速览

```
com.balugaq.eventtrace
├── EventTracePlugin      # 入口：装配 config/recorder/binder/commands
├── config/
│   ├── ConfigManager     # config.yml 读取
│   └── ExemptEvents      # 赦免事件解析（全名 + 简名）
├── gate/
│   ├── ExecutorAccess    # 反射读写 RegisteredListener.executor（1.17 兼容）
│   ├── GateExecutor      # 包装后的 executor：idle 快路 / 计数 / 暂停（后续阶段）
│   └── ListenerBinder    # 全量包装 + 幂等 + 解绑还原
├── trace/
│   ├── TraceRecorder     # 频率计数（ConcurrentHashMap + AtomicLong）
│   └── BreakpointManager # 断点管理（STAGE 3 完善）
├── lang/                 # LangRegistry（内置 yml）+ Lang（静态门面）
└── commands/EtraceCommands # ACF 命令树，权限 etrace.admin 默认 op
```

## 纪律

1. **权限**：所有命令默认仅 op（`etrace.admin`，`default: op`），不要放开默认值。
2. **行为改变边界**：只有「设了断点且命中」才允许改变游戏逻辑；exempt 事件永远快路。
3. **自身免疫**：本插件不注册任何 Bukkit 事件监听（只包装别人的）。
4. **注释**：只写「为什么」，不复述代码。
5. **测试**：新功能必须带测试；跑在 MockBukkit v1.21（4.116.3）上，注意它与 paper-api 1.21.11 配套。
6. **多语言**：所有用户可见文本走 Lang 键（`zh_CN` + `en_US` 两份都要加）。
7. **提交边界**：AI 不 commit/push，由主人拍板。

# HANDOFF — 交接文档

> 最后更新：2026-10-25（STAGE 1 完成）。阶段计划唯一事实来源：[STAGE.md](../STAGE.md)。

## 这是什么

EventTracer：Paper 插件，用于调试事件触发顺序、取消归因、频率监控与游戏内断点单步。目标运行环境 **Paper 1.17+（最低运行版本 1.17）**，Java 17 字节码。

设计依据：`D:/github_clone/Paper/test-plugin/EVENT_TRACE_PLAN.md`（机制论证，未入仓库；STAGE.md 内有摘要）。

## 当前状态（STAGE 1 ✅）

- **构建**：Gradle 9.8.1 wrapper、`gradle/libs.versions.toml` 集中版本、shadowJar 产物 `build/libs/EventTracer-<version>.jar`、`./gradlew clean build` 全绿（15/15 测试通过）。
- **核心机制（idle 档）**：`ListenerBinder` 全量包装所有 `RegisteredListener` 的 executor 为 `GateExecutor`（幂等、定时 rebind 默认 5 分钟、onDisable 还原原 executor）；debug 关 = volatile 快路，零行为改变；debug 开 = dispatch 级去重频率计数；exempt 事件（默认 `PlayerMoveEvent`/`EntityAirChangeEvent`/`BlockPhysicsEvent`，支持全名或简名）永远快路。
- **命令**（ACF `co.aikar:acf-bukkit 0.5.1-SNAPSHOT`，权限 `etrace.admin` 默认 op）：`/etrace`、`/etrace debug on|off`、`/etrace stats [top]`、`/etrace stats reset`、`/etrace reload`。
- **多语言**：`LangRegistry`（内置 `zh_CN`/`en_US`，首次启动复制到 `plugins/EventTracer/lang/`，缺键回退内置），参考 JustEnoughGuide 的 Lang 实现（去 slimefun 依赖）。
- **测试**：JUnit 5.14.4 + MockBukkit v1.21（4.116.3）。覆盖 Lang 解析、exempt 解析、Gate 快路/去重计数、Binder 幂等与解绑、插件加载冒烟。
- **工程化**：CI（build.yml）、自动发版（release.yml，监听 `build.gradle.kts` 版本变更）、CODEOWNERS（`*.java @balugaq`）、dependabot（monthly，ignore paper-api）、CODE_OF_CONDUCT.md（复制自 NetworksExpansion）。

## 已知偏差与坑（重要，别踩第二次）

1. **测试跑在 MockBukkit 1.21 mock 上，而插件目标是 1.17**。原因：MockBukkit 官方无 v1.17 线，最低 v1.21。缓解：主代码编译期锁定 paper-api 1.17.1（`options.release=17`），用到的 Bukkit API 子集（`RegisteredListener` 构造器、`HandlerList`、`EventExecutor`）已用 javap 对 1.17.1 与 1.21.1 双版本核对签名一致。
2. **测试 paper-api 是 1.21.11 而非 1.21.1**：MockBukkit 4.116.3 需要 `org.bukkit.inventory.meta.components.CustomModelDataComponent` 等较新类，1.21.1 快照 jar 里没有，`ClassNotFoundException`。toml 里 `paper`（主）与 `paperTest`（测试）必须分开。
3. **Paper 1.17 的 `RegisteredListener` 无公开 `getExecutor()`**（1.21.1 才有）：拿原 executor 走反射私有字段，见 `gate/ExecutorAccess.java`。升级最低运行版本到 1.21+ 后可换公开 API。
4. **`org.bukkit.event.Listener`/`EventException` 在 `org.bukkit.event` 包**，不是 `org.bukkit.plugin`（1.17/1.21 都是）。
5. **ACF 在 aikar 自有仓库**（`https://repo.aikar.co/content/groups/aikar/`），Maven Central 没有；`acf-bukkit 0.5.1-SNAPSHOT`（SNAPSHOT 内容会漂移，锁 jar 时用时间戳版本）。
6. **`processResources` 只 expand `plugin.yml`**：语言文件里有 `{0}` 占位符，全局 expand 会被 Gradle 模板引擎当 `${}` 处理破坏内容。
7. **`EventTracePlugin` 不能是 `final`**：MockBukkit 用 ByteBuddy 子类化加载插件，final 类直接失败。
8. **本地网络**：`services.gradle.org` 不可达（被墙），wrapper 升级需离线合成或代理；papermc/maven central/aikar 可达。CI（GitHub Actions）不受影响。
9. **runServer 开发服是 MC 26.2**（`minecraftVersion("26.2")`），与最低运行版本 1.17 解耦——只是本地调试环境，不代表支持上限。

## 快速上手（接手人视角）

```bash
./gradlew clean build   # 全绿基线
./gradlew runServer     # 起开发服（自动 eula=true、拷插件 jar）
# 服内：/etrace debug on → 触发事件 → /etrace stats
```

代码入口：`src/main/java/com/balugaq/eventtrace/EventTracePlugin.java`（装配），机制类在 `gate/`（包装与快路）、`trace/`（计数/断点）。

## 下一步（STAGE 2/3/4）

- STAGE 2：`TraceRecorder` 环形缓冲流水 + 取消归因（`isCancelled` 前后跳变），命令 `record`/`last`/`rebind`，`--stack`（StackWalker）。
- STAGE 3（主人 UX 核心）：`/etrace breakpoint <EventClass>`（全名/简名自动匹配 + tab 补全）、命中输出可交互运行列表（hover 步过、click 翻页，每页 10 条）、`step`/`continue`/`cancel`/`inspect`（回放式续行，见规划书 §2.2/§4.3）。
- STAGE 4：方法名反射（`TimedEventExecutor` 私有 `Method` 字段）、长跑无泄漏验证、i18n 补全、文档终稿。

## 验收状态

- [x] `./gradlew clean build` 通过（编译 + 测试 + shadowJar）
- [x] 15/15 测试通过
- [x] shadowJar 含插件类 + ACF + 语言资源，classifier 为空
- [ ] 开服目视验收（idle 开销、stats 数据）——需主人真实服务器确认

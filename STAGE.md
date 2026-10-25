# EventTracer 阶段计划（STAGE.md）

> 本文档是开发计划的**唯一事实来源**。每个阶段独立可验收；当前进度见各阶段标题旁的 ✅/⬜。
> 设计依据：`EVENT_TRACE_PLAN.md`（机制论证）+ 主人 2026-10-25 的需求补充（ACF/Lang、断点 UX、exempt 配置、工程化）。

## 总览

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| STAGE 1 | 工程骨架 + 构建体系 + Gate idle 快路 + 频率统计 + 多语言 + ACF 命令骨架 + 测试基座 + GitHub 工程化 + 全套文档 | ✅ 已完成（2026-10-25） |
| STAGE 2 | TraceRecorder 流水记录 + 取消归因 + `/etrace record`、`/etrace last`、`/etrace rebind` | ⬜ 待办 |
| STAGE 3 | 断点 + 暂停 + 单步（主人 UX 核心）：`/etrace breakpoint`、运行列表分页/hover/click、`step`/`continue`/`cancel`/`inspect` | ⬜ 待办 |
| STAGE 4 | 打磨：`--stack`、方法名反射开关、解绑零残留验证、i18n 补全、文档终稿、长跑验证 | ⬜ 待办 |

---

## STAGE 1 工程骨架与统计（✅ 已完成）

**目标**：插件能编译、能加载、`/etrace stats` 有数据、debug 关闭时零行为改变；工程化（CI/发版/CODEOWNERS/dependabot）与文档齐备。

### 交付物
- **构建体系**：Gradle 9.8.1（wrapper）、Java 17（`options.release=17`，无 toolchain 依赖）、Paper API `1.17.1-R0.1-SNAPSHOT`（最低运行 1.17，`api-version: 1.17`）、依赖版本集中于 `gradle/libs.versions.toml`、shadow 打包、run-paper 开发服（MC 26.2，与最低运行版本 1.17 解耦）。
- **包结构**：`com.balugaq.eventtrace`（config / gate / trace / lang / commands）。
- **配置**（`config.yml`）：`language`、`exempt-events`（事件全名列表，默认 `PlayerMoveEvent`、`EntityAirChangeEvent`、`BlockPhysicsEvent`；兼容简名）、`rebind-interval-minutes`（默认 5）。
- **Gate 机制（idle 档）**：
  - `ListenerBinder`：`ServerLoadEvent` + `onEnable` + 定时 rebind 全量包装所有 `RegisteredListener`；幂等（已是 Gate 的跳过）；`onDisable` 用保存的原 executor 映射解绑，零残留。
  - `GateExecutor`：debug 关且无断点 → volatile 快路直通原 executor；debug 开 → 频率计数（ThreadLocal 按 dispatch 去重，一个 dispatch 只记一次）；exempt 事件直接快路跳过（不记录、不暂停）。
- **TraceRecorder**：频率计数器（`ConcurrentHashMap` + `AtomicLong`）、debug 开关（volatile）。
- **多语言**：`Lang`/`LangRegistry`（参考 JustEnoughGuide，去 slimefun 依赖），内置 `zh_CN` + `en_US`，首次启动复制到 `plugins/EventTracer/lang/`，缺键回退内置。
- **命令**（ACF，`@CommandAlias("etrace")`，权限 `etrace.admin` 默认 op）：
  - `/etrace`（帮助）、`/etrace debug on|off`、`/etrace stats [top]`、`/etrace stats reset`、`/etrace reload`。
- **测试**：JUnit 5.14.4 + MockBukkit v1.21（4.116.3，官方无 v1.17 线，见 HANDOFF「已知偏差」）。覆盖 Lang 解析、exempt 解析、Gate 快路/去重计数、Binder 幂等与解绑、插件加载冒烟。
- **GitHub 工程化**：`.github/workflows/build.yml`（JDK 17 + `gradlew build` 跑测试）、`release.yml`（push 触及 `build.gradle.kts` 且 tag 不存在 → 自动建 Release 上传 jar）、`CODEOWNERS`（`*.java @//balugaq/`）、`dependabot.yml`（monthly，屏蔽 paper-api）、`CODE_OF_CONDUCT.md`（复制自 NetworksExpansion）。
- **文档**：`README.md`、`AGENTS.md`、`docs/HANDOFF.md`、`STAGE.md`（本文件）。

### 验收标准
- `./gradlew clean build` 通过（编译 + 全部测试 + shadowJar）。
- 开服 `/etrace stats` 有数据；`/etrace debug off`  tick 无可感知变化（idle 快路）。
- rebind 幂等：重复执行不重复包装（测试断言 executor 不被二次包装）。
- 解绑后服务端行为与装前一致（测试断言原 executor 还原）。

---

## STAGE 2 流水记录与取消归因（⬜）

**目标**：能回答「最近发生了什么、谁取消了事件」。

### 交付物
- `TraceRecorder` 环形缓冲（默认 4096 条，可配；`/etrace last` 直读，不落盘）。
- `GateExecutor` 记录态：dispatch 级去重计数 + 监听器逐条流水（插件/监听器类/优先级/方法名[可选]/耗时近似/取消前后快照）。
- **取消归因**：真 handler 调用前后各读一次 `isCancelled()`，false→true 的跳变记取消者 `(插件, 监听器类, 优先级)`。
- 命令：`/etrace record on|off [--stack]`、`/etrace last [n] [--event E] [--plugin P] [--cancelled]`、`/etrace rebind`。
- `--stack` 用 StackWalker 记录触发栈（默认关；开启时命令输出警告成本）。
- 测试：造 BlockBreakEvent 被取消场景，`/etrace last --cancelled` 指出取消者；环形缓冲容量边界。

### 验收标准
- 复现「某监听器取消 BlockBreakEvent」→ last 报表正确点名取消者。
- `record off` 后只有计数、无流水（性能回到近 idle）。

---

## STAGE 3 断点与单步（⬜，主人 UX 核心）

**目标**：事件链上打断点，命中后输出可交互的运行列表，可逐监听器步过、可放行。

### 交付物
- `BreakpointManager`：断点条件 = 事件类（或 `*`）+ `--plugin P` + `--priority` + `--on-cancel`；`/etrace breakpoint <EventClass> [filters]`、`/etrace breaks`、`/etrace unbreak <id|all>`。
- **事件类名解析**：支持全名 `org.bukkit.event.player.PlayerMoveEvent` 与简名 `PlayerMoveEvent`；简名唯一匹配自动补全（多个同名 → 列出候选让用户消歧）；命令 Tab 补全给出全名。
- `PauseContext` + `StepController`（回放式续行，机制见 EVENT_TRACE_PLAN §2.2/§4.3）：
  - 命中断点 → 建暂停上下文，剩余监听器被 Gate 吞咽；原始 dispatch 正常结束，主线程自由。
  - `/etrace step [n]` 手动回放快照里接下来 n 个监听器（复制 `RegisteredListener.callEvent` 的 cancelled/ignoreCancelled 语义）；`/etrace continue` 全量放行；`/etrace cancel` 立即取消该事件（仅 Cancellable）；`/etrace inspect` dump 事件字段/取消态/剩余监听器。
  - 异常路径按服务端口径：SEVERE 日志 + `ServerExceptionEvent`，不逃进命令处理器。
  - 仅同步事件可暂停（`Bukkit.isPrimaryThread()` 断言）；异步事件只记录。
- **命中输出 UX（主人指定格式）**：
  ```
  === xxxEvent 运行列表 ===
  1. xxxPlugin - xxxListener - xxxMethod
  2. xxxPlugin - xxxListener - xxxMethod
  ...（每页最多 10 条）
  === 上一页 / 下一页 （当前页数/总页数）===
  ```
  - 每条 listener：`HoverEvent` 显示「步过这个」，`ClickEvent`（runCommand `/etrace step <序号>`）执行步过。
  - 「上一页 / 下一页」：`ClickEvent` 翻页（`/etrace page <n>`）。
  - 输出对象：断点设置者（玩家离线则回落控制台）；暂停期间同类事件再命中 → 记录 + 放行 + 报告（规划书 §4.3 默认行为）。
- `EventInspector`：反射 dump 事件字段（含父类，直到 `Event`）。
- 测试：MockBukkit 造双监听器场景，设断点 → 命中后第二个监听器不执行；step 1 → 执行；continue → 收尾；exempt 事件不命中断点。

### 验收标准
- 断点停在指定监听器前；inspect 状态正确；step 逐个放行；continue 收尾。
- 异常监听器不炸命令栈。
- 分页/hover/click 在真实客户端可点（需主人在开发服目视确认）。

---

## STAGE 4 打磨与收尾（⬜）

- 方法名反射（`TimedEventExecutor` 私有 `Method` 字段，默认关，`--with-method` 开；失败回退 `插件#监听器类@优先级`）。
- rebind 定时策略调优；长跑 30 分钟无泄漏（内存、RL 映射）。
- onDisable 解绑零残留实测。
- i18n 全量补齐（所有用户可见文本走 Lang 键）。
- 文档终稿；规划书 §9 未决项逐条了结。

---

## 贯穿所有阶段的纪律

1. **权限**：所有命令默认仅 op（`etrace.admin`，`default: op`），不放开。
2. **行为改变边界**：只有「设了断点且命中」才改变游戏逻辑；暂停是本质不是 bug，但每次暂停输出必须大字警告，且 `/etrace continue` 永远可用。
3. **exempt 事件**：配置里的 exempt 列表永远走快路（不记录、不命中断点），`PlayerMoveEvent` 等高频事件默认在内。
4. **自身免疫**：本插件不监听任何 Bukkit 事件（绑定/解绑只用 HandlerList API，不走 registerEvent），命令走 ACF，不存在自触发。
5. **注释纪律**：只写「为什么」，不复述代码。
6. **提交边界**：AI 不 commit/push；每阶段结束给出建议 commit 信息，由主人拍板。

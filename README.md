# EventTracer

调试事件触发顺序、取消归因、频率监控与游戏内断点单步的 Paper 插件。

> 状态：STAGE 1 已完成（工程骨架 + 频率统计 + 多语言 + ACF 命令）。阶段计划见 [STAGE.md](STAGE.md)。

## 环境要求

| 项 | 值 |
| --- | --- |
| 服务器 | Paper 1.17+（`api-version: 1.17`，最低运行版本 1.17） |
| Java | 17+（构建用 Gradle 9.8.1，测试编译需 JDK 21） |
| 构建 | `./gradlew clean build`，产物在 `build/libs/EventTracer-<version>.jar` |

## 安装

把 `build/libs/EventTracer-<version>.jar` 放进服务器 `plugins/` 目录，重启。首次启动会复制 `lang/zh_CN.yml`、`lang/en_US.yml` 到 `plugins/EventTracer/lang/`，可自行修改。

## 命令

所有命令权限为 `etrace.admin`，**默认仅 OP**（plugin.yml 中 `default: op`）。

| 命令 | 说明 |
| --- | --- |
| `/etrace` | 显示命令帮助 |
| `/etrace debug on\|off` | 开关调试。关闭后 Gate 回到 idle 快路，零可感知开销 |
| `/etrace stats [top]` | 事件频率统计（按触发次数排序，默认前 20） |
| `/etrace stats reset` | 清空频率统计 |
| `/etrace reload` | 重新加载 `config.yml` 与语言文件 |

> 计划中的命令（STAGE 2/3）：`/etrace record`、`/etrace last`、`/etrace breakpoint <EventClass>`（支持事件全名 `org.bukkit.xxx.event.XxxEvent` 或简名自动匹配）、`/etrace step`、`/etrace continue`、`/etrace cancel`、`/etrace inspect`。断点命中后输出可交互运行列表（hover 步过、点击翻页）。

## 配置（config.yml）

```yaml
# 语言：zh_CN / en_US
language: zh_CN

# 调试总开关（默认 off = idle 快路，零开销）
debug: false

# 全量重新包装所有监听器的时间间隔（分钟）；0 表示关闭定时重新包装
rebind-interval-minutes: 5

# 赦免事件：这些事件永远走快路，不记录、不命中断点。
# 支持事件全名（org.bukkit.xxx.event.XxxEvent）或简名（XxxEvent）。
exempt-events:
  - PlayerMoveEvent
  - EntityAirChangeEvent
  - BlockPhysicsEvent
```

## 它是怎么工作的

- 插件启动/服务器加载完成/定时 rebind 时，遍历全部 `HandlerList`，把每个 `RegisteredListener` 的 executor 包装为 `GateExecutor`（全量包装，幂等）。
- **idle 快路**：debug 关闭且无断点时，`GateExecutor` 只是一个 volatile 判断后直通原 executor。
- **频率统计**：debug 开启后，按 dispatch 级去重计数（一个事件 dispatch 只记一次，而非每个监听器一次）。
- **赦免事件**：命中 `exempt-events` 的事件直接走快路，永不记录、永不暂停。
- 插件自身不监听任何 Bukkit 事件，无自触发。

## 开发

```bash
./gradlew build          # 编译 + 测试 + shadowJar
./gradlew test           # 仅测试（JUnit 5 + MockBukkit）
./gradlew runServer      # 本地开发服（MC 26.2，调试端口 5005）
```

- 依赖版本集中在 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)。
- CI：push/PR 自动编译测试（[.github/workflows/build.yml](.github/workflows/build.yml)）；`build.gradle.kts` 中版本变更且对应 tag 不存在时自动发 Release（[.github/workflows/release.yml](.github/workflows/release.yml)）。
- 依赖自动更新：dependabot（monthly，已屏蔽 paper-api）。

## 参与贡献

提交 PR 请先通过 `./gradlew build`。行为准则见 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。

## License

见 [LICENSE](LICENSE)（如未提供，请联系作者）。

package com.balugaq.eventtrace;

import co.aikar.commands.BukkitCommandManager;
import com.balugaq.eventtrace.commands.EtraceCommands;
import com.balugaq.eventtrace.config.ConfigManager;
import com.balugaq.eventtrace.config.ExemptEvents;
import com.balugaq.eventtrace.gate.ListenerBinder;
import com.balugaq.eventtrace.lang.Lang;
import com.balugaq.eventtrace.trace.BreakpointManager;
import com.balugaq.eventtrace.trace.TraceRecorder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * EventTracer 入口。
 * <p>
 * 职责：装配各组件、注册 ACF 命令、生命周期钩子。本插件刻意不监听任何 Bukkit 事件——
 * 全量包装通过 {@link ListenerBinder} 直接操作 {@link org.bukkit.event.HandlerList} 完成，
 * 因此不存在「自己触发自己被包装」的递归，命令走 ACF 也不产生事件。
 */
public final class EventTracePlugin extends JavaPlugin {
    private static EventTracePlugin instance;

    private ConfigManager configManager;
    private ExemptEvents exemptEvents;
    private TraceRecorder recorder;
    private BreakpointManager breakpoints;
    private ListenerBinder binder;
    private EtraceCommands commands;
    private BukkitCommandManager commandManager;

    public static EventTracePlugin instance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        exemptEvents = new ExemptEvents();
        exemptEvents.load(configManager.getExemptEvents());

        recorder = new TraceRecorder(configManager.isDebugDefault());
        breakpoints = new BreakpointManager();
        binder = new ListenerBinder(this, recorder, breakpoints, exemptEvents);

        Lang.load(this);

        commandManager = new BukkitCommandManager(this);
        commands = new EtraceCommands(this, recorder, configManager, binder);
        commandManager.registerCommand(commands);

        // 初始绑定延后一 tick：确保同载插件的 onEnable 已经执行完毕、监听器注册完成。
        // 不使用 ServerLoadEvent 监听，以遵守「不监听任何事件」的设计纪律。
        Bukkit.getScheduler().runTaskLater(this, binder::bindAll, 1L);

        long minutes = configManager.getRebindIntervalMinutes();
        if (minutes > 0) {
            long ticks = minutes * 60L * 20L;
            Bukkit.getScheduler().runTaskTimer(this, binder::bindAll, ticks, ticks);
        }

        getLogger().info("EventTracer enabled (debug=" + recorder.isDebugEnabled()
                + ", exempt=" + configManager.getExemptEvents().size() + ")");
    }

    @Override
    public void onDisable() {
        if (binder != null) {
            binder.unbindAll();
        }
        if (commandManager != null) {
            commandManager.unregisterCommands();
        }
        Lang.unload();
        getLogger().info("EventTracer disabled.");
    }

    public ExemptEvents getExemptEvents() {
        return exemptEvents;
    }
}

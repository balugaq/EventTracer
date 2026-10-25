package com.balugaq.eventtrace.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.balugaq.eventtrace.EventTracePlugin;
import com.balugaq.eventtrace.config.ConfigManager;
import com.balugaq.eventtrace.config.ExemptEvents;
import com.balugaq.eventtrace.gate.ListenerBinder;
import com.balugaq.eventtrace.lang.Lang;
import com.balugaq.eventtrace.trace.TraceRecorder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code /etrace} 命令树。所有子命令默认仅 op（权限 {@code etrace.admin}，见 plugin.yml）。
 */
@CommandAlias("etrace")
public class EtraceCommands extends BaseCommand {

    private final EventTracePlugin plugin;
    private final TraceRecorder recorder;
    private final ConfigManager configManager;
    private final ListenerBinder binder;

    public EtraceCommands(EventTracePlugin plugin, TraceRecorder recorder,
                          ConfigManager configManager, ListenerBinder binder) {
        this.plugin = plugin;
        this.recorder = recorder;
        this.configManager = configManager;
        this.binder = binder;
    }

    @Default
    @CatchUnknown
    @CommandPermission("etrace.admin")
    @Description("显示 EventTracer 命令帮助")
    public void onHelp(CommandSender sender) {
        Lang.sendMessage(sender, "commands.help");
    }

    @Subcommand("debug")
    @CommandPermission("etrace.admin")
    @Syntax("<on|off>")
    @Description("开关调试。关闭后 Gate 回到 idle 快路，零可感知开销")
    public void onDebug(CommandSender sender, @Single String state) {
        boolean on = state.equalsIgnoreCase("on")
                || state.equalsIgnoreCase("true")
                || state.equalsIgnoreCase("1");
        recorder.setDebug(on);
        Lang.sendMessage(sender, on ? "commands.debug.on" : "commands.debug.off");
    }

    @Subcommand("stats")
    @CommandPermission("etrace.admin")
    @Syntax("[top]")
    @Description("事件频率统计（按触发次数排序）")
    public void onStats(CommandSender sender, @Optional Integer top) {
        int n = top == null ? 20 : top;
        List<Map.Entry<String, AtomicLong>> entries = recorder.top(n);
        Lang.sendMessage(sender, "commands.stats.header", n, recorder.distinctCount());
        if (entries.isEmpty()) {
            Lang.sendMessage(sender, "commands.stats.empty");
            return;
        }
        int index = 1;
        for (Map.Entry<String, AtomicLong> entry : entries) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Lang.t("commands.stats.line", index, entry.getKey(), entry.getValue())));
            index++;
        }
    }

    @Subcommand("stats reset")
    @CommandPermission("etrace.admin")
    @Description("清空频率统计")
    public void onStatsReset(CommandSender sender) {
        recorder.reset();
        Lang.sendMessage(sender, "commands.stats.reset");
    }

    @Subcommand("reload")
    @CommandPermission("etrace.admin")
    @Description("重新加载配置与语言文件")
    public void onReload(CommandSender sender) {
        configManager.load();
        ExemptEvents exempt = plugin.getExemptEvents();
        exempt.load(configManager.getExemptEvents());
        Lang.load(plugin);
        Lang.sendMessage(sender, "commands.reload.done");
        Lang.sendMessage(sender, "exempt.count", configManager.getExemptEvents().size());
    }
}

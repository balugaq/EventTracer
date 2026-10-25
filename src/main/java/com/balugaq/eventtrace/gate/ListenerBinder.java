package com.balugaq.eventtrace.gate;

import com.balugaq.eventtrace.config.ExemptEvents;
import com.balugaq.eventtrace.trace.BreakpointManager;
import com.balugaq.eventtrace.trace.TraceRecorder;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 遍历 {@link HandlerList#getHandlerLists()} 把所有 {@link RegisteredListener} 的 executor
 * 换成 {@link GateExecutor}（全量包装）。幂等、可定时 rebind，并在禁用时还原为原 executor。
 */
public final class ListenerBinder {
    private final Plugin plugin;
    private final TraceRecorder recorder;
    private final BreakpointManager breakpoints;
    private final ExemptEvents exemptEvents;
    private final Map<RegisteredListener, Wrapped> wrapped = new ConcurrentHashMap<>();

    public ListenerBinder(Plugin plugin, TraceRecorder recorder,
                          BreakpointManager breakpoints, ExemptEvents exemptEvents) {
        this.plugin = plugin;
        this.recorder = recorder;
        this.breakpoints = breakpoints;
        this.exemptEvents = exemptEvents;
    }

    /**
     * 全量包装一遍。对已包装的（executor 已是 GateExecutor）直接跳过，因此可反复调用（幂等）。
     */
    public void bindAll() {
        int count = 0;
        for (HandlerList list : HandlerList.getHandlerLists()) {
            for (RegisteredListener rl : list.getRegisteredListeners()) {
                if (wrap(list, rl)) {
                    count++;
                }
            }
        }
        if (count > 0) {
            plugin.getLogger().info("EventTracer 本轮包装了 " + count + " 个监听器。");
        }
    }

    private boolean wrap(HandlerList list, RegisteredListener rl) {
        EventExecutor executor = ExecutorAccess.getExecutor(rl);
        if (executor instanceof GateExecutor) {
            return false; // 已经是我们的 Gate，幂等跳过
        }
        EventExecutor gate = new GateExecutor(executor, rl, recorder, breakpoints, exemptEvents);
        RegisteredListener replacement = new RegisteredListener(
                rl.getListener(), gate, rl.getPriority(), rl.getPlugin(), rl.isIgnoringCancelled());
        list.unregister(rl);
        list.register(replacement);
        wrapped.put(replacement, new Wrapped(rl, list));
        return true;
    }

    /**
     * 还原全部包装：用保存的原始 executor 重建 {@link RegisteredListener} 并替换回原 HandlerList，
     * 使服务端行为在插件卸载后与装载前一致（零残留）。
     */
    public void unbindAll() {
        for (Map.Entry<RegisteredListener, Wrapped> entry : wrapped.entrySet()) {
            RegisteredListener w = entry.getKey();
            Wrapped meta = entry.getValue();
            EventExecutor original = ExecutorAccess.getExecutor(meta.original);
            RegisteredListener restored = new RegisteredListener(
                    w.getListener(), original, w.getPriority(), w.getPlugin(), w.isIgnoringCancelled());
            meta.list.unregister(w);
            meta.list.register(restored);
        }
        wrapped.clear();
        plugin.getLogger().info("EventTracer 已解除全部监听器包装。");
    }

    public int wrappedCount() {
        return wrapped.size();
    }

    private static final class Wrapped {
        final RegisteredListener original;
        final HandlerList list;

        Wrapped(RegisteredListener original, HandlerList list) {
            this.original = original;
            this.list = list;
        }
    }
}

package com.balugaq.eventtrace.gate;

import com.balugaq.eventtrace.config.ExemptEvents;
import com.balugaq.eventtrace.trace.BreakpointManager;
import com.balugaq.eventtrace.trace.TraceRecorder;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;

/**
 * 包装在任意 {@link RegisteredListener} 外层的总闸门。
 * <p>
 * 三种状态：
 * <ol>
 *   <li><b>idle 快路</b>：debug 关闭且无断点 → 一个 volatile 读后直通原 executor，零可感知开销。</li>
 *   <li><b>记录态</b>：dispatch 级去重计数（见 {@link TraceRecorder}）。</li>
 *   <li><b>断点态</b>：STAGE 3 实现；当前 {@link BreakpointManager#hasAny()} 恒为 false。</li>
 * </ol>
 * 赦免事件无论何种状态都直接快路跳过。
 */
public final class GateExecutor implements EventExecutor {
    private final EventExecutor delegate;
    private final RegisteredListener registration;
    private final TraceRecorder recorder;
    private final BreakpointManager breakpoints;
    private final ExemptEvents exemptEvents;

    public GateExecutor(EventExecutor delegate, RegisteredListener registration,
                        TraceRecorder recorder, BreakpointManager breakpoints, ExemptEvents exemptEvents) {
        this.delegate = delegate;
        this.registration = registration;
        this.recorder = recorder;
        this.breakpoints = breakpoints;
        this.exemptEvents = exemptEvents;
    }

    public EventExecutor getDelegate() {
        return delegate;
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        // 赦免事件：永远快路，不记录、不暂停。
        if (exemptEvents.isExempt(event)) {
            delegate.execute(listener, event);
            return;
        }

        // idle 快路：debug 关闭且没有任何断点 → 直通原 executor。
        if (!recorder.isDebugEnabled() && !breakpoints.hasAny()) {
            delegate.execute(listener, event);
            return;
        }

        // 记录态：频率计数（dispatch 级去重）+ 调用原 executor。
        recorder.beforeDispatch(event, registration);
        try {
            delegate.execute(listener, event);
        } finally {
            recorder.afterDispatch(event, registration);
        }
    }
}

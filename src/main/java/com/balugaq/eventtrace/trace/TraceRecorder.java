package com.balugaq.eventtrace.trace;

import org.bukkit.event.Event;
import org.bukkit.plugin.RegisteredListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 频率计数器 + debug 总开关。STAGE 2 将在此追加环形流水缓冲与取消归因（afterDispatch）。
 * <p>
 * dispatch 级去重：同一个事件实例在一次派发中会经过多个 Gate，但只应计数一次。
 * 用 {@link WeakHashMap} 做「当前线程本窗口见过的事件」集合——事件实例被 GC 后条目自动消失，
 * 不会随服务器运行时间增长而泄漏。
 */
public final class TraceRecorder {
    private volatile boolean debug = false;
    private final ConcurrentHashMap<String, AtomicLong> counts = new ConcurrentHashMap<>();
    private final ThreadLocal<Map<Event, Boolean>> seen =
            ThreadLocal.withInitial(() -> new WeakHashMap<>());

    public TraceRecorder(boolean debugDefault) {
        this.debug = debugDefault;
    }

    public void setDebug(boolean value) {
        this.debug = value;
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    /**
     * 在真 handler 调用前调用：dispatch 级去重后累加该事件类的触发次数。
     */
    public void beforeDispatch(Event event, RegisteredListener registration) {
        Map<Event, Boolean> window = seen.get();
        if (window.putIfAbsent(event, Boolean.TRUE) == null) {
            counts.computeIfAbsent(event.getClass().getName(), k -> new AtomicLong()).incrementAndGet();
        }
    }

    /**
     * 在真 handler 调用后调用。STAGE 2 在此读取 isCancelled 前后快照做取消归因。
     */
    public void afterDispatch(Event event, RegisteredListener registration) {
        // STAGE 2 实现
    }

    public List<Map.Entry<String, AtomicLong>> top(int n) {
        return counts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .limit(n)
                .collect(Collectors.toList());
    }

    public int distinctCount() {
        return counts.size();
    }

    public long totalDispatches() {
        long sum = 0;
        for (AtomicLong value : counts.values()) {
            sum += value.get();
        }
        return sum;
    }

    public void reset() {
        counts.clear();
    }
}

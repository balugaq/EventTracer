package com.balugaq.eventtrace.gate;

import com.balugaq.eventtrace.SampleEvent;
import com.balugaq.eventtrace.config.ExemptEvents;
import com.balugaq.eventtrace.trace.BreakpointManager;
import com.balugaq.eventtrace.trace.TraceRecorder;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListenerBinderTest {

    @BeforeEach
    void setUp() {
        if (org.mockbukkit.mockbukkit.MockBukkit.isMocked()) {
            org.mockbukkit.mockbukkit.MockBukkit.unmock();
        }
        org.mockbukkit.mockbukkit.MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        org.mockbukkit.mockbukkit.MockBukkit.unmock();
    }

    private RegisteredListener onlyListener() {
        return SampleEvent.getHandlerList().getRegisteredListeners()[0];
    }

    @Test
    void wrapsOriginalExecutorWithGateAndIsIdempotent() {
        Plugin plugin = org.mockbukkit.mockbukkit.MockBukkit.createMockPlugin();
        EventExecutor original = new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) {
                // 被测监听器，无需动作
            }
        };
        Listener listener = new Listener() {
        };
        org.mockbukkit.mockbukkit.MockBukkit.getMock().getPluginManager()
                .registerEvent(SampleEvent.class, listener, EventPriority.NORMAL, original, plugin);

        TraceRecorder recorder = new TraceRecorder(false);
        ListenerBinder binder = new ListenerBinder(plugin, recorder, new BreakpointManager(), new ExemptEvents());

        // 首次包装
        binder.bindAll();
        RegisteredListener wrapped = onlyListener();
        assertInstanceOf(GateExecutor.class, ExecutorAccess.getExecutor(wrapped), "应被 GateExecutor 包装");

        // 幂等：再次 bindAll 不应产生第二个包装
        binder.bindAll();
        RegisteredListener stillOne = onlyListener();
        assertInstanceOf(GateExecutor.class, ExecutorAccess.getExecutor(stillOne), "二次 bindAll 后仍是 Gate");
        assertTrue(stillOne == wrapped, "幂等：同一个被包装 RL 不应被二次替换");
    }

    @Test
    void unbindRestoresOriginalExecutor() {
        Plugin plugin = org.mockbukkit.mockbukkit.MockBukkit.createMockPlugin();
        final EventExecutor original = new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) {
            }
        };
        Listener listener = new Listener() {
        };
        org.mockbukkit.mockbukkit.MockBukkit.getMock().getPluginManager()
                .registerEvent(SampleEvent.class, listener, EventPriority.NORMAL, original, plugin);

        TraceRecorder recorder = new TraceRecorder(false);
        ListenerBinder binder = new ListenerBinder(plugin, recorder, new BreakpointManager(), new ExemptEvents());
        binder.bindAll();
        assertTrue(ExecutorAccess.getExecutor(onlyListener()) instanceof GateExecutor);

        binder.unbindAll();
        RegisteredListener restored = onlyListener();
        assertFalse(ExecutorAccess.getExecutor(restored) instanceof GateExecutor, "解绑后不应再是 Gate");
        assertSame(original, ExecutorAccess.getExecutor(restored), "解绑应还原为原始 executor 实例");
    }
}

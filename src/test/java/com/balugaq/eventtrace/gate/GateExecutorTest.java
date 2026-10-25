package com.balugaq.eventtrace.gate;

import com.balugaq.eventtrace.SampleEvent;
import com.balugaq.eventtrace.config.ExemptEvents;
import com.balugaq.eventtrace.trace.BreakpointManager;
import com.balugaq.eventtrace.trace.TraceRecorder;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GateExecutorTest {

    private static final class CountExecutor implements EventExecutor {
        int calls = 0;

        @Override
        public void execute(Listener listener, Event event) throws EventException {
            calls++;
        }
    }

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

    private RegisteredListener rl(EventExecutor delegate) {
        JavaPlugin plugin = org.mockbukkit.mockbukkit.MockBukkit.createMockPlugin();
        Listener dummy = new Listener() {
        };
        return new RegisteredListener(dummy, delegate, EventPriority.NORMAL, plugin, false);
    }

    @Test
    void idleFastPathDoesNotCountAndRunsDelegate() throws EventException {
        TraceRecorder recorder = new TraceRecorder(false);
        BreakpointManager breakpoints = new BreakpointManager();
        ExemptEvents exempt = new ExemptEvents();
        exempt.load(List.of());
        CountExecutor delegate = new CountExecutor();
        RegisteredListener registration = rl(delegate);

        GateExecutor gate = new GateExecutor(delegate, registration, recorder, breakpoints, exempt);
        gate.execute(registration.getListener(), new SampleEvent());

        assertEquals(1, delegate.calls);
        assertEquals(0, recorder.totalDispatches());
    }

    @Test
    void debugOnCountsDispatchOnceDespiteMultipleGates() throws EventException {
        TraceRecorder recorder = new TraceRecorder(true);
        BreakpointManager breakpoints = new BreakpointManager();
        ExemptEvents exempt = new ExemptEvents();
        exempt.load(List.of());
        CountExecutor delegate = new CountExecutor();
        RegisteredListener registration = rl(delegate);

        GateExecutor gate = new GateExecutor(delegate, registration, recorder, breakpoints, exempt);
        SampleEvent event = new SampleEvent();
        // 同一事件实例经过两个 Gate（模拟一次派发的两个监听器）
        gate.execute(registration.getListener(), event);
        gate.execute(registration.getListener(), event);

        assertEquals(2, delegate.calls);
        assertEquals(1, recorder.totalDispatches());
    }

    @Test
    void exemptEventSkipsCountEvenWhenDebugOn() throws EventException {
        TraceRecorder recorder = new TraceRecorder(true);
        BreakpointManager breakpoints = new BreakpointManager();
        ExemptEvents exempt = new ExemptEvents();
        exempt.load(List.of("SampleEvent"));
        CountExecutor delegate = new CountExecutor();
        RegisteredListener registration = rl(delegate);

        GateExecutor gate = new GateExecutor(delegate, registration, recorder, breakpoints, exempt);
        gate.execute(registration.getListener(), new SampleEvent());

        assertEquals(1, delegate.calls);
        assertEquals(0, recorder.totalDispatches());
    }
}

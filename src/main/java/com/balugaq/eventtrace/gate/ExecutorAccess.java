package com.balugaq.eventtrace.gate;

import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 跨版本读取 {@link RegisteredListener} 内部持有的 {@link EventExecutor}。
 * <p>
 * Paper 1.18+ 提供了公开的 {@code getExecutor()}；1.17.1 只有私有字段 {@code executor}，
 * 需反射读取。两者构造器签名一致，因此包装/解绑逻辑无需版本分支。
 */
final class ExecutorAccess {
    private static final Method GET_EXECUTOR;
    private static final Field EXECUTOR_FIELD;

    static {
        Method m = null;
        Field f = null;
        try {
            m = RegisteredListener.class.getMethod("getExecutor");
        } catch (NoSuchMethodException ignored) {
            m = null;
        }
        if (m == null) {
            try {
                f = RegisteredListener.class.getDeclaredField("executor");
                f.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
                f = null;
            }
        }
        GET_EXECUTOR = m;
        EXECUTOR_FIELD = f;
    }

    private ExecutorAccess() {
    }

    static EventExecutor getExecutor(RegisteredListener listener) {
        if (GET_EXECUTOR != null) {
            try {
                return (EventExecutor) GET_EXECUTOR.invoke(listener);
            } catch (Exception ignored) {
                // 落到字段读取
            }
        }
        if (EXECUTOR_FIELD != null) {
            try {
                return (EventExecutor) EXECUTOR_FIELD.get(listener);
            } catch (Exception e) {
                throw new IllegalStateException("无法读取 RegisteredListener 的 executor（版本不兼容）", e);
            }
        }
        throw new IllegalStateException("无法读取 RegisteredListener 的 executor（本服务端版本既无 getExecutor 也无 executor 字段）");
    }
}

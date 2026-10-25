package com.balugaq.eventtrace.trace;

/**
 * 断点管理器（STAGE 3 实现）。
 * <p>
 * STAGE 1 仅作为 Gate 的「是否有任何断点」判断占位：当前恒为 false，保证 debug 关闭时走 idle 快路。
 */
public final class BreakpointManager {

    public boolean hasAny() {
        return false;
    }
}

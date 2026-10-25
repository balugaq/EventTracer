package com.balugaq.eventtrace;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 测试用事件：无附加状态，仅供绑定/计数类测试构造实例。
 */
public class SampleEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}

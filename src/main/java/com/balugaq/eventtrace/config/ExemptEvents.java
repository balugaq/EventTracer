package com.balugaq.eventtrace.config;

import org.bukkit.event.Event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 赦免事件表：命中的事件永远走 Gate 快路（不记录、不命中断点）。
 * <p>
 * 配置项 {@code exempt-events} 支持两种写法：
 * <ul>
 *   <li>全名（含点，如 {@code org.bukkit.event.player.PlayerMoveEvent}）：通过 {@link Class#forName} 解析，
 *       命中其规范名；解析失败则退化为按简名匹配最后一段。</li>
 *   <li>简名（如 {@code PlayerMoveEvent}）：按 {@link Class#getSimpleName()} 匹配。</li>
 * </ul>
 */
public final class ExemptEvents {
    private final Set<String> fullNames = new HashSet<>();
    private final Set<String> simpleNames = new HashSet<>();

    public void load(List<String> raw) {
        fullNames.clear();
        simpleNames.clear();
        if (raw == null) {
            return;
        }
        for (String entry : raw) {
            if (entry == null) {
                continue;
            }
            String name = entry.trim();
            if (name.isEmpty()) {
                continue;
            }
            if (name.contains(".")) {
                try {
                    Class<?> clazz = Class.forName(name);
                    if (Event.class.isAssignableFrom(clazz)) {
                        fullNames.add(clazz.getName());
                        continue;
                    }
                } catch (ClassNotFoundException ignored) {
                    // 退化为简名匹配最后一段
                }
                simpleNames.add(lastSegment(name));
            } else {
                simpleNames.add(name);
            }
        }
    }

    public boolean isExempt(Event event) {
        Class<?> clazz = event.getClass();
        if (fullNames.contains(clazz.getName())) {
            return true;
        }
        return simpleNames.contains(clazz.getSimpleName());
    }

    private static String lastSegment(String fqcn) {
        int idx = fqcn.lastIndexOf('.');
        return idx >= 0 ? fqcn.substring(idx + 1) : fqcn;
    }
}

package com.balugaq.eventtrace.config;

import com.balugaq.eventtrace.EventTracePlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * 读取并缓存 {@code config.yml} 中的运行参数。每次 {@code /etrace reload} 或启动时被调用。
 */
public final class ConfigManager {
    private static final List<String> DEFAULT_EXEMPT = List.of(
            "PlayerMoveEvent",
            "EntityAirChangeEvent",
            "BlockPhysicsEvent"
    );

    private final EventTracePlugin plugin;
    private String language = "zh_CN";
    private boolean debugDefault = false;
    private int rebindIntervalMinutes = 5;
    private List<String> exemptEvents = DEFAULT_EXEMPT;

    public ConfigManager(EventTracePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        language = config.getString("language", "zh_CN");
        debugDefault = config.getBoolean("debug", false);
        rebindIntervalMinutes = config.getInt("rebind-interval-minutes", 5);
        List<String> raw = config.getStringList("exempt-events");
        exemptEvents = raw.isEmpty() ? DEFAULT_EXEMPT : List.copyOf(raw);
    }

    public String getLanguage() {
        return language;
    }

    public boolean isDebugDefault() {
        return debugDefault;
    }

    public int getRebindIntervalMinutes() {
        return rebindIntervalMinutes;
    }

    public List<String> getExemptEvents() {
        return exemptEvents;
    }
}

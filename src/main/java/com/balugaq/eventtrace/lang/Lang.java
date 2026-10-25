package com.balugaq.eventtrace.lang;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 多语言静态门面。用法：
 * <pre>
 * import static com.balugaq.eventtrace.lang.Lang.*;
 * sendMessage(sender, "commands.reload.done");
 * sender.sendMessage(t("prefix") + t("something", arg));
 * </pre>
 * 运行时只有一种全局语言（由 {@code config.yml} 的 {@code language} 决定），不需要按玩家切换。
 */
public final class Lang {
    private static final String DEFAULT_LANGUAGE = "zh_CN";
    private static volatile LangRegistry registry;

    private Lang() {
    }

    /**
     * 加载语言文件。须在读过 {@code language} 键之后调用（即 ConfigManager.load 之后）。
     */
    public static void load(JavaPlugin plugin) {
        String language = plugin.getConfig().getString("language", DEFAULT_LANGUAGE);
        File folder = new File(plugin.getDataFolder(), "lang");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("无法创建语言目录 " + folder.getPath() + "，将回退到内置语言文件");
        }

        File file = new File(folder, language + ".yml");
        if (!file.exists() && plugin.getResource("lang/" + language + ".yml") != null) {
            plugin.saveResource("lang/" + language + ".yml", false);
        } else if (!file.exists()) {
            // 配置的语言既无磁盘文件也无内置资源 → 回退默认语言
            language = DEFAULT_LANGUAGE;
            file = new File(folder, language + ".yml");
            if (!file.exists() && plugin.getResource("lang/" + language + ".yml") != null) {
                plugin.saveResource("lang/" + language + ".yml", false);
            }
        }

        FileConfiguration user = loadFile(file);
        FileConfiguration defaults = loadResource(plugin, "lang/" + language + ".yml");
        registry = new LangRegistry(user, defaults, msg -> plugin.getLogger().warning(msg));
        plugin.getLogger().info("EventTracer 语言已加载: " + language);
    }

    public static void unload() {
        registry = null;
    }

    public static boolean isLoaded() {
        return registry != null;
    }

    public static void sendMessage(CommandSender sender, String key, Object... args) {
        LangRegistry current = registry;
        if (current == null) {
            sender.sendMessage(key);
            return;
        }
        for (String line : current.lines(key, args)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    public static String t(String key, Object... args) {
        LangRegistry current = registry;
        if (current == null) {
            return key;
        }
        return ChatColor.translateAlternateColorCodes('&', current.text(key, args));
    }

    public static List<String> lines(String key, Object... args) {
        LangRegistry current = registry;
        if (current == null) {
            return List.of(key);
        }
        return current.lines(key, args).stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .toList();
    }

    private static FileConfiguration loadFile(File file) {
        if (!file.exists()) {
            return new YamlConfiguration();
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            return new YamlConfiguration();
        }
    }

    private static FileConfiguration loadResource(JavaPlugin plugin, String path) {
        YamlConfiguration configuration = new YamlConfiguration();
        try (InputStream in = plugin.getResource(path)) {
            if (in != null) {
                configuration.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            // 内置资源缺失：缺键时由 LangRegistry 告警
        }
        return configuration;
    }
}

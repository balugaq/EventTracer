package com.balugaq.eventtrace.lang;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.function.Consumer;

/**
 * 单层语言注册表：用户磁盘文件优先，内置 jar 资源兜底（仅缺键时回退，绝不写回磁盘）。
 * <p>
 * 占位符格式为 {@code {0}}…{@code {n}}，按顺序替换。语言文件中的 {@code &} 色码由 {@link Lang} 统一上色。
 */
final class LangRegistry {
    private final FileConfiguration user;
    private final FileConfiguration defaults;
    private final Consumer<String> onWarn;

    LangRegistry(FileConfiguration user, FileConfiguration defaults, Consumer<String> onWarn) {
        this.user = user;
        this.defaults = defaults;
        this.onWarn = onWarn;
    }

    String text(String key, Object... args) {
        String raw = user.getString(key);
        if (raw == null) {
            raw = defaults.getString(key);
        }
        if (raw == null) {
            warn(key);
            return key;
        }
        return format(raw, args);
    }

    List<String> lines(String key, Object... args) {
        List<String> list = null;
        if (user.isList(key)) {
            list = user.getStringList(key);
        } else if (defaults.isList(key)) {
            list = defaults.getStringList(key);
        }
        if (list != null && !list.isEmpty()) {
            return list.stream().map(line -> format(line, args)).toList();
        }

        String raw = user.getString(key);
        if (raw == null) {
            raw = defaults.getString(key);
        }
        if (raw == null) {
            warn(key);
            return List.of(key);
        }
        return List.of(format(raw, args));
    }

    private void warn(String key) {
        if (onWarn != null) {
            onWarn.accept("缺少语言键: " + key);
        }
    }

    private static String format(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", args[i] == null ? "null" : args[i].toString());
        }
        return result;
    }
}

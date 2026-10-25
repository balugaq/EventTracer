package com.balugaq.eventtrace.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangRegistryTest {

    @Test
    void resolvesStringWithPlaceholder() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("greet", "Hello {0}!");
        LangRegistry registry = new LangRegistry(new YamlConfiguration(), defaults, null);
        assertEquals("Hello World!", registry.text("greet", "World"));
    }

    @Test
    void userOverridesDefault() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("k", "user-value");
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("k", "default-value");
        LangRegistry registry = new LangRegistry(user, defaults, null);
        assertEquals("user-value", registry.text("k"));
    }

    @Test
    void missingKeyReturnsKeyItself() {
        LangRegistry registry = new LangRegistry(new YamlConfiguration(), new YamlConfiguration(), null);
        assertEquals("nope.key", registry.text("nope.key"));
    }

    @Test
    void listValuesReturnedAsLinesWithFormatting() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("lines", List.of("a {0}", "b"));
        LangRegistry registry = new LangRegistry(new YamlConfiguration(), defaults, null);
        List<String> lines = registry.lines("lines", "x");
        assertEquals(List.of("a x", "b"), lines);
    }

    @Test
    void multiplePlaceholdersReplacedInOrder() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("multi", "{0} {1} {0}");
        LangRegistry registry = new LangRegistry(new YamlConfiguration(), defaults, null);
        assertTrue(registry.text("multi", "A", "B").equals("A B A"));
    }
}

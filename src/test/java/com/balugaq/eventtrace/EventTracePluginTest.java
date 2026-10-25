package com.balugaq.eventtrace;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventTracePluginTest {

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

    @Test
    void pluginLoadsAndCommandsDispatchWithoutError() {
        EventTracePlugin plugin = org.mockbukkit.mockbukkit.MockBukkit.load(EventTracePlugin.class);
        assertNotNull(plugin);

        // 推进一 tick，执行 onEnable 中延后一 tick 的 bindAll
        org.mockbukkit.mockbukkit.MockBukkit.getMock().getScheduler().performOneTick();

        ConsoleCommandSender sender = org.mockbukkit.mockbukkit.MockBukkit.getMock().getConsoleSender();
        sender.addAttachment(plugin, "etrace.admin", true);

        assertDoesNotThrow(() -> Bukkit.getServer().dispatchCommand(sender, "etrace"));
        assertDoesNotThrow(() -> Bukkit.getServer().dispatchCommand(sender, "etrace stats"));
        assertDoesNotThrow(() -> Bukkit.getServer().dispatchCommand(sender, "etrace stats reset"));
        assertDoesNotThrow(() -> Bukkit.getServer().dispatchCommand(sender, "etrace debug on"));

        plugin.onDisable();
    }
}

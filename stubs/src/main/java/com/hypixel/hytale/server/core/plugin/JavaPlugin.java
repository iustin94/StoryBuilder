package com.hypixel.hytale.server.core.plugin;

import java.nio.file.Path;

import com.hypixel.hytale.server.core.command.CommandRegistry;

public abstract class JavaPlugin {

    public JavaPlugin(JavaPluginInit init) {
    }

    public void onSetup() {}
    public void onEnable() {}
    public void onDisable() {}

    public Path getDataFolder() {
        return Path.of(".");
    }

    public CommandRegistry getCommandRegistry() {
        return null;
    }
}

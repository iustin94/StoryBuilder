package com.hypixel.hytale.server.core.plugin;

import java.nio.file.Path;

public abstract class JavaPlugin {

    public JavaPlugin(JavaPluginInit init) {
    }

    public void onSetup() {}
    public void onEnable() {}
    public void onDisable() {}

    public Path getDataFolder() {
        return Path.of(".");
    }
}

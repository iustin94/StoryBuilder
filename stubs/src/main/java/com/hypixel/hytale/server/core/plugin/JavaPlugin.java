package com.hypixel.hytale.server.core.plugin;

import java.io.File;

public abstract class JavaPlugin {
    public abstract void onInitialize();

    public File getDataFolder() {
        return new File(".");
    }
}

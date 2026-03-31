package net.questforge.prefabs;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.nio.file.Path;
import java.util.logging.Logger;

public class PrefabBrowserPlugin extends JavaPlugin {

    private static final Logger LOGGER = Logger.getLogger("PrefabBrowser");

    private PrefabRegistry registry;
    private boolean initialized = false;

    public PrefabBrowserPlugin(JavaPluginInit init) {
        super(init);
        initialize();
    }

    private void initialize() {
        if (initialized) return;
        initialized = true;

        try {
            LOGGER.info("PrefabBrowser v1.0 initializing...");

            Path dataFolder = Path.of("mods", "PrefabBrowser");
            dataFolder.toFile().mkdirs();

            // Default prefab directory — can be overridden via config in future
            Path prefabDir = Path.of("assets", "prefabs");
            registry = new PrefabRegistry(prefabDir);
            registry.reload();

            // Register the /prefabs command
            if (getCommandRegistry() != null) {
                getCommandRegistry().register("prefabs", new PrefabCommand(registry));
                LOGGER.info("Registered /prefabs command");
            }

            LOGGER.info("PrefabBrowser initialized with " + registry.getAll().size() + " prefabs");
        } catch (Exception e) {
            LOGGER.severe("PrefabBrowser failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

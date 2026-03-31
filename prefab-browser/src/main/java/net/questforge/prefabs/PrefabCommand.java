package net.questforge.prefabs;

import com.hypixel.hytale.server.core.command.CommandHandler;
import com.hypixel.hytale.server.core.player.PlayerRef;

import net.questforge.prefabs.ui.PrefabBrowserPage;

/**
 * Handles the /prefabs command. Opens the prefab browser UI for the player.
 * Usage: /prefabs [search query]
 */
public class PrefabCommand implements CommandHandler {

    private final PrefabRegistry registry;

    public PrefabCommand(PrefabRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void execute(PlayerRef player, String[] args) {
        PrefabBrowserPage page = new PrefabBrowserPage(player, registry);

        if (args.length > 0) {
            String search = String.join(" ", args);
            player.sendMessage("[PrefabBrowser] Opening with filter: " + search);
            page.show(search);
        } else {
            player.sendMessage("[PrefabBrowser] Opening prefab browser...");
            page.show();
        }
    }
}

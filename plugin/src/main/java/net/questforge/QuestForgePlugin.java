package net.questforge;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.packet.PacketRegistry;
import com.hypixel.hytale.server.packet.SyncInteractionChain;
import net.questforge.config.AssignmentConfig;
import net.questforge.config.PlayerDataStore;
import net.questforge.engine.ConditionEvaluator;
import net.questforge.engine.QuestLoader;
import net.questforge.engine.QuestManager;
import net.questforge.http.QuestForgeHttpServer;
import net.questforge.hytale.DialogueUI;
import net.questforge.hytale.NpcInteractListener;
import net.questforge.hytale.ObjectiveBridge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

public class QuestForgePlugin extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("QuestForge");
    private static final int DEFAULT_PORT = 7432;

    private QuestForgeHttpServer httpServer;
    private QuestManager questManager;

    @Override
    public void onInitialize() {
        LOGGER.info("QuestForge v1.0 initializing...");

        Path dataFolder = getDataFolder().toPath();

        // 1. Create config/persistence
        AssignmentConfig assignmentConfig = new AssignmentConfig(dataFolder);
        PlayerDataStore playerDataStore = new PlayerDataStore(dataFolder);

        // 2. Create engine components
        ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
        QuestLoader questLoader = new QuestLoader(dataFolder.resolve("quests"));

        // 3. Create Hytale integration components
        DialogueUI dialogueUI = new DialogueUI();
        ObjectiveBridge objectiveBridge; // created after QuestManager

        // 4. Create QuestManager
        questManager = new QuestManager(
            playerDataStore, assignmentConfig, conditionEvaluator,
            dialogueUI, null, questLoader  // objectiveBridge set after
        );

        // 5. Wire circular dependencies
        objectiveBridge = new ObjectiveBridge(questManager);
        questManager.setObjectiveBridge(objectiveBridge);
        dialogueUI.setQuestManager(questManager);

        // 6. Load quests
        questManager.reload();
        LOGGER.info("Loaded " + questManager.getQuests().size() + " quests");

        // 7. Register NPC interaction listener
        NpcInteractListener npcListener = new NpcInteractListener(questManager);
        PacketRegistry.register(SyncInteractionChain.class, npcListener);

        // 8. Start HTTP server
        try {
            httpServer = new QuestForgeHttpServer(DEFAULT_PORT, questManager, assignmentConfig, questLoader);
            httpServer.start();
            LOGGER.info("HTTP server started on port " + DEFAULT_PORT);
        } catch (IOException e) {
            LOGGER.severe("Failed to start HTTP server: " + e.getMessage());
        }

        LOGGER.info("QuestForge initialized successfully!");
    }
}

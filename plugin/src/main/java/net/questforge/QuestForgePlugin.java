package net.questforge;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import net.questforge.config.AssignmentConfig;
import net.questforge.config.PlayerDataStore;
import net.questforge.engine.ConditionEvaluator;
import net.questforge.engine.QuestLoader;
import net.questforge.engine.QuestManager;
import net.questforge.http.QuestForgeHttpServer;
import net.questforge.hytale.DialogueUI;
import net.questforge.hytale.ObjectiveBridge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

public class QuestForgePlugin extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("QuestForge");
    private static final int DEFAULT_PORT = 7432;

    private QuestForgeHttpServer httpServer;
    private QuestManager questManager;

    private boolean initialized = false;

    public QuestForgePlugin(JavaPluginInit init) {
        super(init);
        initialize();
    }

    private void initialize() {
        if (initialized) return;
        initialized = true;
        try {
            LOGGER.info("QuestForge v1.0 initializing...");

            Path dataFolder = Path.of("mods", "QuestForge");
            dataFolder.toFile().mkdirs();

            AssignmentConfig assignmentConfig = new AssignmentConfig(dataFolder);
            PlayerDataStore playerDataStore = new PlayerDataStore(dataFolder);

            ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
            QuestLoader questLoader = new QuestLoader(dataFolder.resolve("quests"));

            DialogueUI dialogueUI = new DialogueUI();
            ObjectiveBridge objectiveBridge;

            questManager = new QuestManager(
                playerDataStore, assignmentConfig, conditionEvaluator,
                dialogueUI, null, questLoader
            );

            objectiveBridge = new ObjectiveBridge(questManager);
            questManager.setObjectiveBridge(objectiveBridge);
            dialogueUI.setQuestManager(questManager);

            questManager.reload();
            LOGGER.info("Loaded " + questManager.getQuests().size() + " quests");

            httpServer = new QuestForgeHttpServer(DEFAULT_PORT, questManager, assignmentConfig, questLoader);
            httpServer.start();
            LOGGER.info("HTTP server started on port " + DEFAULT_PORT);

            LOGGER.info("QuestForge initialized successfully!");
        } catch (Exception e) {
            LOGGER.severe("QuestForge failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

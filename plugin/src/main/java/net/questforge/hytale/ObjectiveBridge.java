package net.questforge.hytale;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.questforge.engine.QuestManager;
import net.questforge.model.QuestState;

import com.hypixel.hytale.server.objective.ObjectivePlugin;
import com.hypixel.hytale.server.core.player.PlayerRef;

public class ObjectiveBridge {

    private final QuestManager questManager;
    private final Map<UUID, ObjectiveMapping> activeObjectives;

    public ObjectiveBridge(QuestManager questManager) {
        this.questManager = questManager;
        this.activeObjectives = new ConcurrentHashMap<>();
    }

    /**
     * Starts a Hytale objective for the given player and quest state, and registers
     * a completion callback that feeds back into the QuestManager.
     */
    public void startObjective(PlayerRef player, String questId, QuestState state) {
        UUID objectiveUuid = ObjectivePlugin.get()
                .startObjective(player, state.getObjectiveId());

        ObjectiveMapping mapping = new ObjectiveMapping(
                player, questId, state.getObjectiveId());
        activeObjectives.put(objectiveUuid, mapping);

        ObjectivePlugin.get().onObjectiveComplete(objectiveUuid, () -> onComplete(objectiveUuid));
    }

    /**
     * Called by the ObjectivePlugin callback when an objective is completed.
     */
    private void onComplete(UUID objectiveUuid) {
        ObjectiveMapping mapping = activeObjectives.remove(objectiveUuid);
        if (mapping != null) {
            questManager.onObjectiveCompleted(mapping.player, mapping.objectiveId);
        }
    }

    /**
     * Maps a Hytale objective UUID back to the QuestForge player, quest, and objective.
     */
    private static class ObjectiveMapping {
        final PlayerRef player;
        final String questId;
        final String objectiveId;

        ObjectiveMapping(PlayerRef player, String questId, String objectiveId) {
            this.player = player;
            this.questId = questId;
            this.objectiveId = objectiveId;
        }
    }
}

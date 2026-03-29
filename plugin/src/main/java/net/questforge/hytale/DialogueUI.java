package net.questforge.hytale;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.questforge.engine.QuestManager;
import net.questforge.model.PlayerQuestData;
import net.questforge.model.Quest;
import net.questforge.model.QuestState;
import net.questforge.model.QuestTransition;

import com.hypixel.hytale.server.ui.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.player.PlayerRef;

public class DialogueUI {

    private final Map<String, InteractiveCustomUIPage> activePages;
    private QuestManager questManager;

    public DialogueUI() {
        this.activePages = new ConcurrentHashMap<>();
    }

    public void setQuestManager(QuestManager manager) {
        this.questManager = manager;
    }

    /**
     * Shows a dialogue page for the current quest state, presenting only transitions
     * whose conditions are satisfied.
     */
    public void showDialogue(PlayerRef player, Quest quest, QuestState state,
                             PlayerQuestData.QuestProgress progress) {
        String playerId = player.getUniqueId().toString();

        // Build UI content as HTML
        StringBuilder html = new StringBuilder();
        html.append("<h1>").append(quest.getQuestName()).append("</h1>");
        html.append("<p>").append(state.getDialogue()).append("</p>");

        // Create the interactive page
        InteractiveCustomUIPage page = new InteractiveCustomUIPage() {};
        page.setContent(html.toString());

        // Add event handlers for each available transition
        List<QuestTransition> transitions = state.getTransitions();
        for (int i = 0; i < transitions.size(); i++) {
            QuestTransition transition = transitions.get(i);
            final String targetStateId = transition.getTo();
            String eventId = "transition_" + i;
            page.onEvent(eventId, () -> {
                if (questManager != null) {
                    questManager.onResponseChosen(player, quest.getQuestId(), transition.getId());
                }
            });
        }

        // Open the page for the player and track it
        page.open(player);
        activePages.put(playerId, page);
    }

    /**
     * Shows a selection list of available quests for the player to pick from.
     */
    public void showQuestSelection(PlayerRef player, List<Quest> quests) {
        String playerId = player.getUniqueId().toString();

        StringBuilder html = new StringBuilder();
        html.append("<h1>Available Quests</h1>");
        html.append("<p>Select a quest to begin:</p>");

        InteractiveCustomUIPage page = new InteractiveCustomUIPage() {};
        page.setContent(html.toString());

        for (int i = 0; i < quests.size(); i++) {
            Quest quest = quests.get(i);
            final String questId = quest.getQuestId();
            String eventId = "quest_" + i;
            page.onEvent(eventId, () -> {
                if (questManager != null) {
                    questManager.onNpcInteract(player, questId);
                }
            });
        }

        page.open(player);
        activePages.put(playerId, page);
    }

    /**
     * Closes any active dialogue page for the given player.
     */
    public void close(PlayerRef player) {
        String playerId = player.getUniqueId().toString();
        InteractiveCustomUIPage page = activePages.remove(playerId);
        if (page != null) {
            page.close(player);
        }
    }
}

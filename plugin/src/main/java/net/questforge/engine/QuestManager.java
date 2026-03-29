package net.questforge.engine;

import com.hypixel.hytale.server.core.player.PlayerRef;
import net.questforge.config.AssignmentConfig;
import net.questforge.config.PlayerDataStore;
import net.questforge.hytale.DialogueUI;
import net.questforge.hytale.ObjectiveBridge;
import net.questforge.model.Condition;
import net.questforge.model.PlayerQuestData;
import net.questforge.model.PlayerQuestData.QuestProgress;
import net.questforge.model.Quest;
import net.questforge.model.QuestState;
import net.questforge.model.QuestTransition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestManager {

    private Map<String, Quest> quests;
    private final PlayerDataStore playerDataStore;
    private final AssignmentConfig assignmentConfig;
    private final ConditionEvaluator conditionEvaluator;
    private DialogueUI dialogueUI;
    private ObjectiveBridge objectiveBridge;
    private final QuestLoader questLoader;

    public QuestManager(PlayerDataStore playerDataStore, AssignmentConfig assignmentConfig,
                        ConditionEvaluator conditionEvaluator, DialogueUI dialogueUI,
                        ObjectiveBridge objectiveBridge, QuestLoader questLoader) {
        this.playerDataStore = playerDataStore;
        this.assignmentConfig = assignmentConfig;
        this.conditionEvaluator = conditionEvaluator;
        this.dialogueUI = dialogueUI;
        this.objectiveBridge = objectiveBridge;
        this.questLoader = questLoader;
        this.quests = new HashMap<>();
    }

    public void reload() {
        quests = questLoader.loadAll();
    }

    public void onNpcInteract(PlayerRef player, String npcRole) {
        List<String> assignedIds = assignmentConfig.getQuestsForNpc(npcRole);
        if (assignedIds == null || assignedIds.isEmpty()) {
            return;
        }

        String playerId = player.getUniqueId().toString();
        PlayerQuestData playerData = playerDataStore.getPlayerData(playerId);
        if (playerData == null) {
            playerData = new PlayerQuestData(playerId);
        }

        List<Quest> eligible = new ArrayList<>();
        for (String questId : assignedIds) {
            Quest quest = quests.get(questId);
            if (quest == null) {
                continue;
            }
            QuestProgress progress = playerData.getProgress(questId);
            if (progress == null) {
                // Not started yet - eligible
                eligible.add(quest);
            } else if ("in_progress".equals(progress.getStatus())) {
                // Currently in progress - eligible
                eligible.add(quest);
            }
            // completed or failed quests are not eligible
        }

        if (eligible.isEmpty()) {
            return;
        }

        if (eligible.size() == 1) {
            Quest quest = eligible.get(0);
            QuestProgress progress = playerData.getProgress(quest.getQuestId());
            if (progress == null) {
                // Start the quest
                progress = new QuestProgress(quest.getStartState(), "in_progress",
                        buildInitialVariables(quest), new HashMap<>());
                playerData.setProgress(quest.getQuestId(), progress);
                playerDataStore.savePlayerData(playerData);
                advanceToState(player, quest.getQuestId(), quest.getStartState());
            } else {
                // Resume from current state
                String currentStateId = progress.getCurrentState();
                QuestState currentState = quest.getStates().get(currentStateId);
                if (currentState != null) {
                    dialogueUI.showDialogue(player, quest, currentState, progress);
                }
            }
        } else {
            dialogueUI.showQuestSelection(player, eligible);
        }
    }

    public void onResponseChosen(PlayerRef player, String questId, String transitionId) {
        String playerId = player.getUniqueId().toString();
        PlayerQuestData playerData = playerDataStore.getPlayerData(playerId);
        if (playerData == null) {
            return;
        }

        QuestProgress progress = playerData.getProgress(questId);
        if (progress == null) {
            return;
        }

        Quest quest = quests.get(questId);
        if (quest == null) {
            return;
        }

        QuestState currentState = quest.getStates().get(progress.getCurrentState());
        if (currentState == null) {
            return;
        }

        for (QuestTransition transition : currentState.getTransitions()) {
            if (transition.getId().equals(transitionId)) {
                advanceToState(player, questId, transition.getTo());
                return;
            }
        }
    }

    public void onObjectiveCompleted(PlayerRef player, String objectiveId) {
        String playerId = player.getUniqueId().toString();
        PlayerQuestData playerData = playerDataStore.getPlayerData(playerId);
        if (playerData == null) {
            return;
        }

        for (Map.Entry<String, Quest> entry : quests.entrySet()) {
            String questId = entry.getKey();
            Quest quest = entry.getValue();
            QuestProgress progress = playerData.getProgress(questId);
            if (progress == null || !"in_progress".equals(progress.getStatus())) {
                continue;
            }

            QuestState currentState = quest.getStates().get(progress.getCurrentState());
            if (currentState == null) {
                continue;
            }

            if (currentState.getType() == QuestState.StateType.OBJECTIVE
                    && objectiveId.equals(currentState.getObjectiveId())) {
                String nextState = currentState.getOnObjectiveComplete();
                if (nextState != null) {
                    advanceToState(player, questId, nextState);
                }
                return;
            }
        }
    }

    private void advanceToState(PlayerRef player, String questId, String nextStateId) {
        String playerId = player.getUniqueId().toString();
        PlayerQuestData playerData = playerDataStore.getPlayerData(playerId);
        if (playerData == null) {
            playerData = new PlayerQuestData(playerId);
        }

        Quest quest = quests.get(questId);
        if (quest == null) {
            return;
        }

        QuestProgress progress = playerData.getProgress(questId);
        if (progress == null) {
            progress = new QuestProgress(nextStateId, "in_progress",
                    buildInitialVariables(quest), new HashMap<>());
            playerData.setProgress(questId, progress);
        }

        // Update current state and visit counts
        progress.setCurrentState(nextStateId);
        int visits = progress.getVisitCounts().getOrDefault(nextStateId, 0);
        progress.getVisitCounts().put(nextStateId, visits + 1);

        QuestState state = quest.getStates().get(nextStateId);
        if (state == null) {
            playerDataStore.savePlayerData(playerData);
            return;
        }

        switch (state.getType()) {
            case DIALOGUE:
                dialogueUI.showDialogue(player, quest, state, progress);
                break;

            case START:
                // Treat START like dialogue - show transitions as choices
                dialogueUI.showDialogue(player, quest, state, progress);
                break;

            case OBJECTIVE:
                objectiveBridge.startObjective(player, questId, state);
                break;

            case BRANCH:
                // Evaluate transitions and advance to the first one whose conditions pass
                for (QuestTransition transition : state.getTransitions()) {
                    if (evaluateAllConditions(transition.getConditions(), progress)) {
                        playerDataStore.savePlayerData(playerData);
                        advanceToState(player, questId, transition.getTo());
                        return;
                    }
                }
                // No branch matched - stay in current state
                break;

            case END:
                // Determine outcome from state title or default to completed
                String outcome = state.getTitle();
                if (outcome != null && outcome.toLowerCase().contains("fail")) {
                    progress.setStatus("failed");
                } else {
                    progress.setStatus("completed");
                }
                break;

            default:
                break;
        }

        playerDataStore.savePlayerData(playerData);
    }

    private boolean evaluateAllConditions(List<Condition> conditions, QuestProgress progress) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        for (Condition cond : conditions) {
            if (!conditionEvaluator.evaluate(cond, progress)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> buildInitialVariables(Quest quest) {
        Map<String, Object> vars = new HashMap<>();
        if (quest.getVariables() != null) {
            for (Map.Entry<String, Quest.VariableDefinition> entry : quest.getVariables().entrySet()) {
                vars.put(entry.getKey(), entry.getValue().getInitial());
            }
        }
        return vars;
    }

    public Map<String, Quest> getQuests() {
        return quests;
    }

    public void setDialogueUI(DialogueUI dialogueUI) {
        this.dialogueUI = dialogueUI;
    }

    public void setObjectiveBridge(ObjectiveBridge objectiveBridge) {
        this.objectiveBridge = objectiveBridge;
    }
}

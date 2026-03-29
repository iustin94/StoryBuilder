package net.questforge.model;

import java.util.HashMap;
import java.util.Map;

public class PlayerQuestData {

    private final String playerId;
    private final Map<String, QuestProgress> quests;

    public PlayerQuestData(String playerId) {
        this.playerId = playerId;
        this.quests = new HashMap<>();
    }

    public PlayerQuestData(String playerId, Map<String, QuestProgress> quests) {
        this.playerId = playerId;
        this.quests = quests == null ? new HashMap<>() : new HashMap<>(quests);
    }

    public String getPlayerId() {
        return playerId;
    }

    public Map<String, QuestProgress> getQuests() {
        return quests;
    }

    public QuestProgress getProgress(String questId) {
        return quests.get(questId);
    }

    public void setProgress(String questId, QuestProgress progress) {
        quests.put(questId, progress);
    }

    public Object getVariable(String questId, String varName) {
        QuestProgress progress = quests.get(questId);
        if (progress == null) {
            return null;
        }
        return progress.getVariables().get(varName);
    }

    public void setVariable(String questId, String varName, Object value) {
        QuestProgress progress = quests.get(questId);
        if (progress == null) {
            return;
        }
        progress.getVariables().put(varName, value);
    }

    public static class QuestProgress {

        private String currentState;
        private String status;
        private final Map<String, Object> variables;
        private final Map<String, Integer> visitCounts;

        public QuestProgress(String currentState, String status,
                             Map<String, Object> variables, Map<String, Integer> visitCounts) {
            this.currentState = currentState;
            this.status = status;
            this.variables = variables == null ? new HashMap<>() : new HashMap<>(variables);
            this.visitCounts = visitCounts == null ? new HashMap<>() : new HashMap<>(visitCounts);
        }

        public String getCurrentState() {
            return currentState;
        }

        public void setCurrentState(String currentState) {
            this.currentState = currentState;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public Map<String, Integer> getVisitCounts() {
            return visitCounts;
        }
    }
}

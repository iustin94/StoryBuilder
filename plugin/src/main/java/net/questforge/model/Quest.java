package net.questforge.model;

import java.util.Collections;
import java.util.Map;

public class Quest {

    private final String questId;
    private final String questName;
    private final String startState;
    private final Map<String, VariableDefinition> variables;
    private final Map<String, QuestState> states;

    public Quest(String questId, String questName, String startState,
                 Map<String, VariableDefinition> variables, Map<String, QuestState> states) {
        this.questId = questId;
        this.questName = questName;
        this.startState = startState;
        this.variables = variables == null ? Collections.emptyMap() : Collections.unmodifiableMap(variables);
        this.states = states == null ? Collections.emptyMap() : Collections.unmodifiableMap(states);
    }

    public String getQuestId() {
        return questId;
    }

    public String getQuestName() {
        return questName;
    }

    public String getStartState() {
        return startState;
    }

    public Map<String, VariableDefinition> getVariables() {
        return variables;
    }

    public Map<String, QuestState> getStates() {
        return states;
    }

    public static class VariableDefinition {

        private final String type;
        private final Object initial;

        public VariableDefinition(String type, Object initial) {
            this.type = type;
            this.initial = initial;
        }

        public String getType() {
            return type;
        }

        public Object getInitial() {
            return initial;
        }
    }
}

package net.questforge.model;

import java.util.Collections;
import java.util.List;

public class QuestState {

    public enum StateType {
        START,
        DIALOGUE,
        OBJECTIVE,
        BRANCH,
        END
    }

    private final StateType type;
    private final String title;
    private final String dialogue;
    private final String objectiveId;
    private final String onObjectiveComplete;
    private final List<QuestTransition> transitions;

    public QuestState(StateType type, String title, String dialogue, String objectiveId,
                      String onObjectiveComplete, List<QuestTransition> transitions) {
        this.type = type;
        this.title = title;
        this.dialogue = dialogue;
        this.objectiveId = objectiveId;
        this.onObjectiveComplete = onObjectiveComplete;
        this.transitions = transitions == null ? Collections.emptyList() : Collections.unmodifiableList(transitions);
    }

    public StateType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDialogue() {
        return dialogue;
    }

    public String getObjectiveId() {
        return objectiveId;
    }

    public String getOnObjectiveComplete() {
        return onObjectiveComplete;
    }

    public List<QuestTransition> getTransitions() {
        return transitions;
    }
}

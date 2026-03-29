package net.questforge.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class QuestTransition {

    private final String id;
    private final String label;
    private final String to;
    private final List<Condition> conditions;

    public QuestTransition(String id, String label, String to, List<Condition> conditions) {
        this.id = (id != null) ? id : generateIdFromLabel(label);
        this.label = label;
        this.to = to;
        this.conditions = conditions == null ? Collections.emptyList() : Collections.unmodifiableList(conditions);
    }

    private static String generateIdFromLabel(String label) {
        if (label == null || label.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return label.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getTo() {
        return to;
    }

    public List<Condition> getConditions() {
        return conditions;
    }
}

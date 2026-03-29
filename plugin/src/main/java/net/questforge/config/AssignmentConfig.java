package net.questforge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AssignmentConfig {

    private static final Logger LOGGER = Logger.getLogger(AssignmentConfig.class.getName());
    private static final Type MAP_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();

    private final Path filePath;
    private final Gson gson;
    private Map<String, List<String>> assignments; // npcRole -> [questId, ...]

    public AssignmentConfig(Path dataFolder) {
        this.filePath = dataFolder.resolve("npc-assignments.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public void load() {
        if (Files.exists(filePath)) {
            try (Reader reader = Files.newBufferedReader(filePath)) {
                Map<String, List<String>> loaded = gson.fromJson(reader, MAP_TYPE);
                this.assignments = loaded != null ? loaded : new HashMap<>();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load npc-assignments.json, initializing empty", e);
                this.assignments = new HashMap<>();
            }
        } else {
            this.assignments = new HashMap<>();
        }
    }

    public void save() {
        try {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = Files.newBufferedWriter(filePath)) {
                gson.toJson(assignments, writer);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save npc-assignments.json", e);
        }
    }

    public List<String> getQuestsForNpc(String npcRole) {
        return assignments.getOrDefault(npcRole, new ArrayList<>());
    }

    public List<String> getNpcsForQuest(String questId) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : assignments.entrySet()) {
            if (entry.getValue().contains(questId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public void assign(String npcRole, String questId) {
        List<String> quests = assignments.computeIfAbsent(npcRole, k -> new ArrayList<>());
        if (!quests.contains(questId)) {
            quests.add(questId);
            save();
        }
    }

    public void unassign(String npcRole, String questId) {
        List<String> quests = assignments.get(npcRole);
        if (quests != null && quests.remove(questId)) {
            if (quests.isEmpty()) {
                assignments.remove(npcRole);
            }
            save();
        }
    }

    public void removeQuest(String questId) {
        boolean changed = false;
        Iterator<Map.Entry<String, List<String>>> it = assignments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<String>> entry = it.next();
            if (entry.getValue().remove(questId)) {
                changed = true;
                if (entry.getValue().isEmpty()) {
                    it.remove();
                }
            }
        }
        if (changed) {
            save();
        }
    }

    public Map<String, List<String>> getAll() {
        return assignments;
    }
}

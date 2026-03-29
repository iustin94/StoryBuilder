package net.questforge.engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.questforge.model.Quest;
import net.questforge.model.QuestState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class QuestLoader {

    private final Gson gson;
    private final Path questsDir;

    public QuestLoader(Path questsDir) {
        this.questsDir = questsDir;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(QuestState.StateType.class, new StateTypeAdapter())
                .create();
    }

    public Map<String, Quest> loadAll() {
        Map<String, Quest> quests = new HashMap<>();
        if (!Files.isDirectory(questsDir)) {
            return quests;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(questsDir, "*.json")) {
            for (Path file : stream) {
                try {
                    Quest quest = load(file);
                    if (quest != null && quest.getQuestId() != null) {
                        quests.put(quest.getQuestId(), quest);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load quest file: " + file + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to iterate quest directory: " + questsDir + " - " + e.getMessage());
        }
        return quests;
    }

    public Quest load(Path file) {
        try {
            String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            return gson.fromJson(json, Quest.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read quest file: " + file, e);
        }
    }

    public void save(Quest quest) {
        Path file = questsDir.resolve(quest.getQuestId() + ".json");
        try {
            Files.createDirectories(questsDir);
            String json = gson.toJson(quest);
            Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save quest file: " + file, e);
        }
    }

    public void delete(String questId) {
        Path file = questsDir.resolve(questId + ".json");
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete quest file: " + file, e);
        }
    }

    private static class StateTypeAdapter extends TypeAdapter<QuestState.StateType> {

        @Override
        public void write(JsonWriter out, QuestState.StateType value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.name().toLowerCase());
            }
        }

        @Override
        public QuestState.StateType read(JsonReader in) throws IOException {
            String value = in.nextString();
            if (value == null) {
                return null;
            }
            return QuestState.StateType.valueOf(value.toUpperCase());
        }
    }
}

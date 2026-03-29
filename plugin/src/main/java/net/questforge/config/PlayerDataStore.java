package net.questforge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.questforge.model.PlayerQuestData;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerDataStore {

    private static final Logger LOGGER = Logger.getLogger(PlayerDataStore.class.getName());

    private final Path playerDataDir;
    private final Gson gson;
    private final Map<String, PlayerQuestData> cache;

    public PlayerDataStore(Path dataFolder) {
        this.playerDataDir = dataFolder.resolve("player-data");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cache = new ConcurrentHashMap<>();
        try {
            Files.createDirectories(playerDataDir);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create player-data directory", e);
        }
    }

    public PlayerQuestData getPlayerData(String playerId) {
        PlayerQuestData cached = cache.get(playerId);
        if (cached != null) {
            return cached;
        }

        Path file = playerDataDir.resolve(playerId + ".json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                PlayerQuestData data = gson.fromJson(reader, PlayerQuestData.class);
                if (data != null) {
                    cache.put(playerId, data);
                    return data;
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load player data for " + playerId, e);
            }
        }

        PlayerQuestData newData = new PlayerQuestData(playerId);
        cache.put(playerId, newData);
        return newData;
    }

    public void savePlayerData(PlayerQuestData data) {
        String playerId = data.getPlayerId();
        cache.put(playerId, data);
        Path file = playerDataDir.resolve(playerId + ".json");
        try {
            Files.createDirectories(playerDataDir);
            try (Writer writer = Files.newBufferedWriter(file)) {
                gson.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save player data for " + playerId, e);
        }
    }

    public void clearCache() {
        cache.clear();
    }
}

package com.hypixel.hytale.server.objective;

import com.hypixel.hytale.server.core.player.PlayerRef;
import java.util.UUID;

public class ObjectivePlugin {
    private static final ObjectivePlugin INSTANCE = new ObjectivePlugin();

    public static ObjectivePlugin get() {
        return INSTANCE;
    }

    public UUID startObjective(PlayerRef player, String objectiveId) {
        return UUID.randomUUID();
    }

    public void onObjectiveComplete(UUID objectiveUuid, Runnable callback) {
    }
}

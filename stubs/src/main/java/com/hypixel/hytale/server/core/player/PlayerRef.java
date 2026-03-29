package com.hypixel.hytale.server.core.player;

import java.util.UUID;

public interface PlayerRef {
    UUID getUniqueId();
    String getName();
    void sendMessage(String msg);
}

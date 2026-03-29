package com.hypixel.hytale.server.npc;

import java.util.UUID;

public interface NPCEntity {
    NPCRole getRole();
    UUID getUniqueId();
}

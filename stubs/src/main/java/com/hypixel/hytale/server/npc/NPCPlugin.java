package com.hypixel.hytale.server.npc;

public class NPCPlugin {
    private static final NPCPlugin INSTANCE = new NPCPlugin();

    public static NPCPlugin get() {
        return INSTANCE;
    }

    public NPCBuilderManager getBuilderManager() {
        return null;
    }
}

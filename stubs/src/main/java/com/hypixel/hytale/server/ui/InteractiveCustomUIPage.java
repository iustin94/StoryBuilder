package com.hypixel.hytale.server.ui;

import com.hypixel.hytale.server.core.player.PlayerRef;

public abstract class InteractiveCustomUIPage {
    public void open(PlayerRef player) {
    }

    public void close(PlayerRef player) {
    }

    public void setContent(String html) {
    }

    public void onEvent(String eventId, Runnable handler) {
    }
}

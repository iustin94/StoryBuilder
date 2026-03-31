package com.hypixel.hytale.server.core.command;

import com.hypixel.hytale.server.core.player.PlayerRef;

public interface CommandHandler {
    void execute(PlayerRef player, String[] args);
}

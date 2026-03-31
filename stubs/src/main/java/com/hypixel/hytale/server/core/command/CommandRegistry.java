package com.hypixel.hytale.server.core.command;

public interface CommandRegistry {
    void register(String name, CommandHandler handler);
}

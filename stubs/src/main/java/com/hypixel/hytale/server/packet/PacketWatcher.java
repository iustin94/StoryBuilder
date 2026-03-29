package com.hypixel.hytale.server.packet;

public interface PacketWatcher<T> {
    void onPacket(T packet);
}

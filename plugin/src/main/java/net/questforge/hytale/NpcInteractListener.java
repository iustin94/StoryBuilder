package net.questforge.hytale;

import net.questforge.engine.QuestManager;

import com.hypixel.hytale.server.packet.PacketWatcher;
import com.hypixel.hytale.server.core.player.PlayerRef;
import com.hypixel.hytale.server.packet.SyncInteractionChain;

public class NpcInteractListener implements PacketWatcher<SyncInteractionChain> {

    private final QuestManager questManager;

    public NpcInteractListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public void onPacket(SyncInteractionChain packet) {
        PlayerRef player = packet.getPlayer();
        String npcRole = packet.getNpcEntity().getRole().getName();
        questManager.onNpcInteract(player, npcRole);
    }
}

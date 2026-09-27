package com.gugas749.abysscore.client;

import com.gugas749.abysscore.network.binds.KeyPressPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import com.gugas749.abysscore.network.PacketHandler;

public class ClientTickHandler {
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Forge fires this twice per tick (START and END) — NeoForge's .Post == END
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        // Don't fire if not in-game
        if (mc.player == null || mc.level == null) return;
        // Don't fire while a screen is open
        if (mc.screen != null) return;

        for (int i = 0; i < KeyBindings.SLOT_COUNT; i++) {
            // consumeClick returns true once per physical key press
            if (KeyBindings.SLOTS[i].consumeClick()) {
                PacketHandler.CHANNEL.sendToServer(new KeyPressPacket(i + 1)); // slots are 1-indexed
            }
        }
    }
}

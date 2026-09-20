package com.gugas749.abysscore.client.ui.screens;

import com.gugas749.abysscore.network.blind.BlindSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Renders a full opaque black overlay when the player is blinded.
 *
 * Rendered in BOTH Pre and Post events to cover everything —
 * including chat message backgrounds which are transparent in Post only.
 */
@OnlyIn(Dist.CLIENT)
public class BlindScreen {

    private static boolean blinded = false;

    public static void handleSync(BlindSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> blinded = packet.blinded());
    }

    public static boolean isBlinded() { return blinded; }

    @SubscribeEvent
    public void onRenderGuiPre(RenderGuiEvent.Pre event) {
        renderBlack(event.getGuiGraphics());
    }

    @SubscribeEvent
    public void onRenderGuiPost(RenderGuiEvent.Post event) {
        renderBlack(event.getGuiGraphics());
    }

    private void renderBlack(GuiGraphics g) {
        if (!blinded) return;
        if (Minecraft.getInstance().player == null) return;
        Minecraft mc = Minecraft.getInstance();
        // 0xFF000000 — alpha MUST be FF (fully opaque), 0x000000 alone is transparent
        g.fill(0, 0,
            mc.getWindow().getGuiScaledWidth(),
            mc.getWindow().getGuiScaledHeight(),
            0xFF000000
        );
    }
}

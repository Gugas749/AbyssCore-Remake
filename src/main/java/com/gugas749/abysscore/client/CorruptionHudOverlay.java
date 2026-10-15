package com.gugas749.abysscore.client;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.api.corruption.AbyssCorruption;
import com.gugas749.abysscore.api.corruption.CorruptionData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * ABY-16 — corruption bar, moved from AbyssItems (same texture, same position: left of the hotbar).
 * Hidden while the player has no corruption at all.
 *
 * /corruptionhud toggles it. That's a CLIENT command (RegisterClientCommandsEvent): it runs on
 * your own game, so it works on a dedicated server too. AbyssItems' old /corruption show was a
 * server command flipping a client field, which only worked in singleplayer.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Abysscore.MODID, value = Dist.CLIENT)
public final class CorruptionHudOverlay {

    private static boolean showBar = true;

    private static final ResourceLocation LAYER_ID = Abysscore.asResource("corruption_hud");
    private static final ResourceLocation TEXTURE = Abysscore.asResource("textures/gui/corruption_bar.png");

    // The texture is drawn at 15% scale: 1176×106 → about 176×16 on screen.
    private static final int BAR_WIDTH  = 1176;
    private static final int BAR_HEIGHT = 106;
    private static final int TEX_WIDTH  = 1176;
    private static final int TEX_HEIGHT = 212;   // top half = empty frame, bottom half = fill
    private static final float SCALE    = 0.15f;

    private CorruptionHudOverlay() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("corruptionhud").executes(ctx -> {
            showBar = !showBar;
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "command.abysscore.corruption.hud", showBar ? "visible" : "hidden"), false);
            return 1;
        }));
    }

    /** GUI layers are a MOD bus event → separate subscriber class. */
    @OnlyIn(Dist.CLIENT)
    @EventBusSubscriber(modid = Abysscore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void onRegisterLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, CorruptionHudOverlay::render);
        }
    }

    private static void render(GuiGraphics gfx, DeltaTracker delta) {
        if (!showBar) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        CorruptionData data = AbyssCorruption.data(mc.player);
        if (data.level() == 0 && data.progress() == 0) return;
        int corruption = data.level();

        int scaledWidth  = (int) (BAR_WIDTH  * SCALE);
        int scaledHeight = (int) (BAR_HEIGHT * SCALE);
        int hotbarLeft = (gfx.guiWidth() - 182) / 2;
        int barX = hotbarLeft - scaledWidth - 3;
        int barY = gfx.guiHeight() - 17;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        gfx.pose().pushPose();
        gfx.pose().translate(barX, barY, 0);
        gfx.pose().scale(SCALE, SCALE, 1.0f);
        gfx.blit(TEXTURE, 0, 0, 0, 0, BAR_WIDTH, BAR_HEIGHT, TEX_WIDTH, TEX_HEIGHT);           // frame
        int fillWidth = (int) Math.ceil((corruption / (double) AbyssCorruption.MAX) * BAR_WIDTH);
        if (fillWidth > 0) {
            gfx.blit(TEXTURE, 0, 0, 0, BAR_HEIGHT, fillWidth, BAR_HEIGHT, TEX_WIDTH, TEX_HEIGHT); // fill
        }
        gfx.pose().popPose();

        // Text at normal scale, centred on the bar
        String text = corruption + "%";
        int textX = barX + scaledWidth / 2 - mc.font.width(text) / 2;
        int textY = barY + scaledHeight / 2 - 4;
        gfx.drawString(mc.font, text, textX + 1, textY + 1, 0x44000000, false);
        gfx.drawString(mc.font, text, textX, textY, textColor(corruption), false);

        RenderSystem.disableBlend();
    }

    /** white → light purple → purple → dark purple → dark red at 100%. */
    private static int textColor(int corruption) {
        if (corruption >= 100) return 0xFF4B0000;
        if (corruption >= 75)  return 0xFF6A006A;
        if (corruption >= 50)  return 0xFFAA00AA;
        if (corruption >= 25)  return 0xFFCC88FF;
        return 0xFFFFFFFF;
    }
}

package com.gugas749.abysscore.client;

import com.gugas749.abysscore.api.overlay.AbyssOverlayRenderer;
import com.gugas749.abysscore.client.ui.screens.BlindScreen;
import com.gugas749.abysscore.network.region.NoEntryHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * All client-only registration in one place.
 * Only called from Abysscore when FMLEnvironment.dist == CLIENT, so a dedicated
 * server never loads this class (or the screen/HUD classes it references).
 */
@OnlyIn(Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {}

    public static void init(IEventBus modEventBus) {
        // Key mappings are a MOD bus event
        modEventBus.addListener(KeyBindings::register);

        // HUD overlays are a MOD bus event too (RegisterGuiOverlaysEvent)
        modEventBus.addListener(AbyssOverlayRenderer::registerOverlay);   // ABY-15 tints / vignettes
        modEventBus.addListener(CorruptionHudOverlay::registerOverlay);   // ABY-16 corruption bar

        // Rendering / ticking are GAME (Forge) bus events
        MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
        MinecraftForge.EVENT_BUS.register(new ACVanishHudHandler());
        MinecraftForge.EVENT_BUS.register(new NoEntryHandler());
        MinecraftForge.EVENT_BUS.register(new BlindScreen());
    }
}

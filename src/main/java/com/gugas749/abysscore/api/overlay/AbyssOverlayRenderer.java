package com.gugas749.abysscore.api.overlay;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * CLIENT: draws every active overlay (ABY-15). You never call this directly —
 * use AbyssScreenOverlay on the server / in common code.
 *
 * Drawn as a GUI layer right above vanilla's "camera overlays" (pumpkin blur, powder snow,
 * portal swirl), so it tints the world but sits UNDER the hotbar, chat and other HUD.
 *
 * Timing uses wall-clock milliseconds (50 ms = 1 tick) instead of the player's tickCount,
 * because tickCount restarts at 0 when you respawn or change dimension.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Abysscore.MODID, value = Dist.CLIENT)
public final class AbyssOverlayRenderer {

    private static final ResourceLocation LAYER_ID = Abysscore.asResource("screen_overlay");
    private static final float MS_PER_TICK = 50f;

    /** Overlays started by a packet (show / damage). */
    private static final Map<ResourceLocation, Timed> TIMED = new HashMap<>();

    private static final class Timed {
        final AbyssOverlaySpec spec;
        final long startMs;
        float durationTicks;

        Timed(AbyssOverlaySpec spec, long startMs, float durationTicks) {
            this.spec = spec;
            this.startMs = startMs;
            this.durationTicks = durationTicks;
        }

        float elapsedTicks(long now) {
            return (now - startMs) / MS_PER_TICK;
        }
    }

    private AbyssOverlayRenderer() {}

    // ── Packet ────────────────────────────────────────────────────────────────

    public static void handle(ScreenOverlayPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            long now = Util.getMillis();
            if (packet.durationTicks() > 0) {
                TIMED.put(packet.id(), new Timed(packet.spec(), now, packet.durationTicks()));
                return;
            }
            // Clear: end it "fadeOut ticks from now" so it fades instead of popping off.
            Timed t = TIMED.get(packet.id());
            if (t != null) {
                t.durationTicks = Math.min(t.durationTicks, t.elapsedTicks(now) + t.spec.fadeOutTicks());
            }
        });
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        TIMED.clear();
    }

    /** GUI layers are registered on the MOD event bus, so this needs its own subscriber class. */
    @OnlyIn(Dist.CLIENT)
    @EventBusSubscriber(modid = Abysscore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void onRegisterLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, LAYER_ID, AbyssOverlayRenderer::render);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    private static void render(GuiGraphics g, DeltaTracker delta) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        long now = Util.getMillis();

        // 1) Timed overlays (packets)
        Iterator<Timed> it = TIMED.values().iterator();
        while (it.hasNext()) {
            Timed t = it.next();
            float elapsed = t.elapsedTicks(now);
            float remaining = t.durationTicks - elapsed;
            if (remaining <= 0) { it.remove(); continue; }

            float fadeIn  = t.spec.fadeInTicks()  > 0 ? Math.min(1f, elapsed   / t.spec.fadeInTicks())  : 1f;
            float fadeOut = t.spec.fadeOutTicks() > 0 ? Math.min(1f, remaining / t.spec.fadeOutTicks()) : 1f;
            draw(g, t.spec, t.spec.maxAlpha() * Math.min(fadeIn, fadeOut));
        }

        // 2) Effect overlays — read straight from the player's active effects (the server
        //    already syncs your own effects to you, so no extra packets are needed).
        var bindings = AbyssScreenOverlay.effectOverlays();
        if (bindings.isEmpty()) return;
        for (MobEffectInstance inst : player.getActiveEffects()) {
            var key = inst.getEffect().unwrapKey();
            if (key.isEmpty()) continue;
            AbyssOverlaySpec spec = bindings.get(key.get().location());
            if (spec == null) continue;

            float strength = spec.scaleWithAmplifier() ? Math.min(1f, (inst.getAmplifier() + 1) / 4f) : 1f;
            float fadeOut = 1f;
            if (!inst.isInfiniteDuration() && spec.fadeOutTicks() > 0) {
                fadeOut = Math.min(1f, inst.getDuration() / (float) spec.fadeOutTicks());
            }
            draw(g, spec, spec.maxAlpha() * strength * fadeOut);
        }
    }

    private static void draw(GuiGraphics g, AbyssOverlaySpec spec, float alpha) {
        if (alpha <= 0.003f) return;   // invisible, skip the draw calls
        int w = g.guiWidth(), h = g.guiHeight();
        int full = argb(spec.color(), alpha);

        if (spec.style() == AbyssOverlaySpec.Style.TINT) {
            g.fill(0, 0, w, h, full);
            return;
        }

        // VIGNETTE: colour at the edges fading to transparent towards the centre.
        int clear = argb(spec.color(), 0f);
        int bandY = h / 4;
        g.fillGradient(0, 0, w, bandY, full, clear);          // top    (vertical gradient)
        g.fillGradient(0, h - bandY, w, h, clear, full);      // bottom

        // GuiGraphics only has VERTICAL gradients, so the sides are built from thin strips
        // with decreasing alpha — 16 steps is smooth enough at GUI scale.
        int bandX = w / 5, steps = 16;
        float stripW = bandX / (float) steps;
        for (int i = 0; i < steps; i++) {
            int c = argb(spec.color(), alpha * (1f - i / (float) steps));
            int x0 = Math.round(i * stripW), x1 = Math.round((i + 1) * stripW);
            g.fill(x0, 0, x1, h, c);                          // left
            g.fill(w - x1, 0, w - x0, h, c);                  // right
        }
    }

    private static int argb(int rgb, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}

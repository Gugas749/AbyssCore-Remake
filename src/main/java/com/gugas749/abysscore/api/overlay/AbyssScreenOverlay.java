package com.gugas749.abysscore.api.overlay;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ABY-15 — screen overlays (tints / vignettes) triggered by effects, attacks, or code.
 *
 * Three ways to use it:
 *
 *  1. EFFECT → overlay (shown for as long as the player has the effect, no packets involved):
 *
 *       AbyssScreenOverlay.bindEffect(MyEffects.BLEEDING,
 *               AbyssOverlaySpec.vignette(0xAA0000, 0.5f).scaleWithAmplifier());
 *
 *     Call this from your mod constructor (or common setup) so it runs on BOTH the client and
 *     the server — the client is the one that draws it.
 *
 *  2. DAMAGE TYPE → overlay (flashes whenever a player takes that kind of damage):
 *
 *       AbyssScreenOverlay.bindDamageType(AbyssDamageTypes.CORRUPTION_DAMAGE,
 *               AbyssOverlaySpec.tint(0x5A0080, 0.35f).fade(2, 15), 20);
 *
 *  3. Directly from server code (a skill, a cutscene...):
 *
 *       AbyssScreenOverlay.show(player, MyMod.id("flash"), AbyssOverlaySpec.tint(0xFFFFFF, 0.8f), 10);
 *       AbyssScreenOverlay.clear(player, MyMod.id("flash"));
 *
 * Same id shown again = replaced (and its timer restarts), different ids stack.
 */
@EventBusSubscriber(modid = Abysscore.MODID)
public final class AbyssScreenOverlay {

    /** Effect id → how it looks. Read by the client renderer every frame. */
    private static final Map<ResourceLocation, AbyssOverlaySpec> EFFECT_OVERLAYS = new HashMap<>();

    /** Damage type → (how it looks, how long). Read on the server when a player is hurt. */
    private static final Map<ResourceKey<DamageType>, DamageOverlay> DAMAGE_OVERLAYS = new HashMap<>();

    private record DamageOverlay(ResourceLocation id, AbyssOverlaySpec spec, int durationTicks) {}

    private AbyssScreenOverlay() {}

    // ── Bindings ──────────────────────────────────────────────────────────────

    /**
     * Works with vanilla effects (MobEffects.POISON) and your own DeferredHolder effects.
     * The holder MUST be a registered reference (it needs a registry key) — DeferredHolder is.
     */
    public static void bindEffect(Holder<MobEffect> effect, AbyssOverlaySpec spec) {
        ResourceKey<MobEffect> key = effect.unwrapKey().orElseThrow(() ->
                new IllegalArgumentException("[AbyssCore] bindEffect needs a registered effect holder"));
        EFFECT_OVERLAYS.put(key.location(), spec);
    }

    public static void bindDamageType(ResourceKey<DamageType> type, AbyssOverlaySpec spec, int durationTicks) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                type.location().getNamespace(), "damage/" + type.location().getPath());
        DAMAGE_OVERLAYS.put(type, new DamageOverlay(id, spec, durationTicks));
    }

    /** Read-only view for the client renderer. */
    public static Map<ResourceLocation, AbyssOverlaySpec> effectOverlays() {
        return Collections.unmodifiableMap(EFFECT_OVERLAYS);
    }

    // ── Direct control (server side) ──────────────────────────────────────────

    public static void show(ServerPlayer player, ResourceLocation id, AbyssOverlaySpec spec, int durationTicks) {
        PacketDistributor.sendToPlayer(player, new ScreenOverlayPacket(id, spec, Math.max(1, durationTicks)));
    }

    /** Fades the overlay out (using its fade-out time) instead of cutting it instantly. */
    public static void clear(ServerPlayer player, ResourceLocation id) {
        PacketDistributor.sendToPlayer(player,
                new ScreenOverlayPacket(id, AbyssOverlaySpec.tint(0, 0f), 0));
    }

    // ── Damage hook ───────────────────────────────────────────────────────────

    /** Post = after armor/resistances, and only if damage actually went through. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (DAMAGE_OVERLAYS.isEmpty()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getNewDamage() <= 0) return;

        for (var entry : DAMAGE_OVERLAYS.entrySet()) {
            if (event.getSource().is(entry.getKey())) {
                DamageOverlay o = entry.getValue();
                show(player, o.id(), o.spec(), o.durationTicks());
            }
        }
    }
}

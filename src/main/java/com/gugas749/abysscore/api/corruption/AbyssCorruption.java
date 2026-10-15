package com.gugas749.abysscore.api.corruption;

import com.gugas749.abysscore.api.attachment.AbyssSyncedAttachment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * ABY-16 — the shared corruption system (moved here from AbyssItems).
 *
 * Core owns the MECHANISM: a 0–100 level per player, saved with the player, kept on death,
 * synced to the client and shown as a HUD bar. Mods own the POLICY: what raises it and what
 * it does (AbyssItems: Void Essence gain + strength/dodge; AbyssOrigins: skills that inflict it).
 *
 *   // a skill hits someone with 5 corruption
 *   AbyssCorruption.add(target, 5);
 *
 *   // slow passive gain while wearing an item: +1 level per 86,400 ticks (≈ 1 real day)
 *   AbyssCorruption.addProgress(player, 86_400);
 *
 *   // read it to scale your own effects
 *   int level = AbyssCorruption.get(player);
 *
 * Server side only, except get(Player) which also works on the client (for HUDs).
 */
public final class AbyssCorruption {

    public static final int MAX = 100;

    /** Registered as "abysscore:corruption". Synced to the owning player's client. */
    public static final AbyssSyncedAttachment<CorruptionData> ATTACHMENT =
            AbyssSyncedAttachment.register("corruption", CorruptionData.NONE, CorruptionData.CODEC);

    private AbyssCorruption() {}

    /**
     * Does nothing — but CALLING it makes Java load this class, which runs the ATTACHMENT line
     * above and registers the attachment. Abysscore calls this before the attachment
     * DeferredRegister is attached to the mod bus; a static field that's never touched would
     * otherwise be registered too late (or never).
     */
    public static void init() {}

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Works on both sides (the client copy is kept in sync). */
    public static int get(Player player) {
        return data(player).level();
    }

    public static CorruptionData data(Player player) {
        return player.getData(ATTACHMENT.getAttachmentType());
    }

    // ── Write (server) ────────────────────────────────────────────────────────

    public static void set(ServerPlayer player, int level) {
        CorruptionData old = data(player);
        ATTACHMENT.set(player, new CorruptionData(level, old.progress()));
    }

    /** Negative amounts reduce it. Clamped to 0–100. */
    public static void add(ServerPlayer player, int amount) {
        set(player, get(player) + amount);
    }

    /** Level AND progress back to 0. */
    public static void reset(ServerPlayer player) {
        ATTACHMENT.set(player, CorruptionData.NONE);
    }

    /**
     * Call once per tick to slowly raise corruption: after `ticksPerLevel` calls the level goes
     * up by 1 and progress starts over. Does nothing at MAX.
     *
     * Only syncs to the client when something VISIBLE changes (the level, or the very first
     * progress tick, which makes the bar appear) — not 20 packets a second.
     */
    public static void addProgress(ServerPlayer player, int ticksPerLevel) {
        CorruptionData old = data(player);
        if (old.level() >= MAX) return;

        int progress = old.progress() + 1;
        if (progress >= ticksPerLevel) {
            ATTACHMENT.set(player, new CorruptionData(old.level() + 1, 0));
        } else if (old.progress() == 0) {
            ATTACHMENT.set(player, new CorruptionData(old.level(), progress));
        } else {
            ATTACHMENT.setSilently(player, new CorruptionData(old.level(), progress));
        }
    }
}

package com.gugas749.abysscore.api.network;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;

/**
 * Forge 1.20.1 replacement for NeoForge's {@code IPayloadContext}.
 *
 * Wraps Forge's {@link NetworkEvent.Context} and exposes the same two methods
 * the Abyss handlers use: {@link #enqueueWork} and {@link #player}.
 */
public final class AbyssPacketContext {

    private final NetworkEvent.Context ctx;

    public AbyssPacketContext(NetworkEvent.Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Packets arrive on the NETTY thread. Anything that touches the world, players
     * or screens must be pushed to the main game thread with this.
     */
    public void enqueueWork(Runnable work) {
        ctx.enqueueWork(work);
    }

    /**
     * The player who SENT the packet (server side only). Null on the client.
     *
     * Return type is {@link Player} on purpose, not ServerPlayer: Java 17 refuses to compile
     * {@code ctx.player() instanceof ServerPlayer p} when the expression is already a
     * ServerPlayer ("pattern is always true"). Java 21 allows it, Java 17 does not.
     */
    @Nullable
    public Player player() {
        return ctx.getSender();
    }

    /** Escape hatch to the raw Forge context. */
    public NetworkEvent.Context raw() {
        return ctx;
    }
}

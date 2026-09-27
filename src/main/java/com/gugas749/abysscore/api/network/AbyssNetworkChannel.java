package com.gugas749.abysscore.api.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * One network channel per mod (Forge 1.20.1 {@link SimpleChannel}).
 *
 * On NeoForge you got a registrar from RegisterPayloadHandlersEvent and every packet had a
 * ResourceLocation TYPE. On Forge 1.20.1 packets are identified by an INDEX (0, 1, 2...)
 * inside a channel, so this class hands out the indexes for you.
 *
 * IMPORTANT: packets must be registered in the SAME ORDER on client and server,
 * because the index is what goes over the wire. Registering everything from one
 * method (see PacketHandler) guarantees that.
 *
 * Usage in a consuming mod (e.g. AbyssEvents):
 *
 *   public static final AbyssNetworkChannel CHANNEL =
 *       AbyssNetworkChannel.create(new ResourceLocation("abyssevents", "main"), "1");
 */
public final class AbyssNetworkChannel {

    private final SimpleChannel channel;
    private int nextId = 0;

    private AbyssNetworkChannel(SimpleChannel channel) {
        this.channel = channel;
    }

    /**
     * @param name            unique channel id, e.g. abysscore:main
     * @param protocolVersion bump this when packets change; client and server must match
     */
    public static AbyssNetworkChannel create(ResourceLocation name, String protocolVersion) {
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                name,
                () -> protocolVersion,
                protocolVersion::equals,   // versions the client accepts from the server
                protocolVersion::equals    // versions the server accepts from the client
        );
        return new AbyssNetworkChannel(channel);
    }

    int nextId() {
        return nextId++;
    }

    public SimpleChannel raw() {
        return channel;
    }

    // ── Sending (replaces NeoForge's static PacketDistributor.sendToX) ────────

    public void sendToPlayer(ServerPlayer player, Object packet) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void sendToAll(Object packet) {
        channel.send(PacketDistributor.ALL.noArg(), packet);
    }

    public void sendToServer(Object packet) {
        channel.sendToServer(packet);
    }
}

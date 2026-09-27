package com.gugas749.abysscore.network.vanish;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;

/** Server → Client: tells the client whether they are currently vanished. */
public record VanishStateSyncPacket(boolean vanished) {

    public static final AbyssPacketCodec<VanishStateSyncPacket> CODEC = AbyssPacketCodec.of(
            (buf, pkt) -> buf.writeBoolean(pkt.vanished()),
            buf -> new VanishStateSyncPacket(buf.readBoolean())
    );
}

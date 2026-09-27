package com.gugas749.abysscore.network.dimen;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;

/** Server → Client: open the dimension creation screen. No fields. */
public record OpenDimenCreateScreenPacket() {

    public static final AbyssPacketCodec<OpenDimenCreateScreenPacket> CODEC =
        AbyssPacketCodec.unit(new OpenDimenCreateScreenPacket());
}

package com.gugas749.abysscore.network.menu.packets;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;

/** Client → Server: asks the server to send the region manager screen. No fields. */
public record RequestRegionScreenPacket() {

    public static final AbyssPacketCodec<RequestRegionScreenPacket> CODEC =
            AbyssPacketCodec.unit(new RequestRegionScreenPacket());
}

package com.gugas749.abysscore.network.region;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;

/**
 * Client → Server: blackscreen is at peak opacity, safe to teleport now.
 * Server teleports the player on receiving this — the world change is
 * hidden by the black overlay.
 */
public record ReadyToTeleportPacket() {

    public static final AbyssPacketCodec<ReadyToTeleportPacket> CODEC =
            AbyssPacketCodec.unit(new ReadyToTeleportPacket());
}

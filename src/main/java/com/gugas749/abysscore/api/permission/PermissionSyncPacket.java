package com.gugas749.abysscore.api.permission;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server → Client: "your AbyssCore permission level is now X". */
public record PermissionSyncPacket(AbyssPermissionLevel level) implements CustomPacketPayload {

    public static final Type<PermissionSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "permission_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PermissionSyncPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> buf.writeEnum(pkt.level()),
            buf -> new PermissionSyncPacket(buf.readEnum(AbyssPermissionLevel.class))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

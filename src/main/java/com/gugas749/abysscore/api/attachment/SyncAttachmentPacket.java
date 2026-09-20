package com.gugas749.abysscore.api.attachment;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncAttachmentPacket(ResourceLocation id, byte[] data) implements CustomPacketPayload {

    public static final Type<SyncAttachmentPacket> TYPE =
            new Type<>(Abysscore.asResource("sync_attachment"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAttachmentPacket> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeResourceLocation(pkt.id());
                        buf.writeByteArray(pkt.data());
                    },
                    buf -> new SyncAttachmentPacket(
                            buf.readResourceLocation(),
                            buf.readByteArray()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

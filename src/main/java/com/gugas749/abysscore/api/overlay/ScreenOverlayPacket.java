package com.gugas749.abysscore.api.overlay;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → Client: "show overlay `id` for `durationTicks`", or clear it when durationTicks <= 0.
 * Sent by AbyssScreenOverlay.show/clear; never construct it yourself.
 */
public record ScreenOverlayPacket(ResourceLocation id, AbyssOverlaySpec spec, int durationTicks)
        implements CustomPacketPayload {

    public static final Type<ScreenOverlayPacket> TYPE = new Type<>(Abysscore.asResource("screen_overlay"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenOverlayPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeResourceLocation(pkt.id());
                buf.writeInt(pkt.spec().color());
                buf.writeFloat(pkt.spec().maxAlpha());
                buf.writeEnum(pkt.spec().style());
                buf.writeVarInt(pkt.spec().fadeInTicks());
                buf.writeVarInt(pkt.spec().fadeOutTicks());
                buf.writeVarInt(pkt.durationTicks());
            },
            buf -> new ScreenOverlayPacket(
                    buf.readResourceLocation(),
                    new AbyssOverlaySpec(buf.readInt(), buf.readFloat(),
                            buf.readEnum(AbyssOverlaySpec.Style.class),
                            buf.readVarInt(), buf.readVarInt(), false),
                    buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

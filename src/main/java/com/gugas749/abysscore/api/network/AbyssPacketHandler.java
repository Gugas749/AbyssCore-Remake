package com.gugas749.abysscore.api.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.function.Supplier;

public class AbyssPacketHandler {

    /*
     *           EXAMPLE PACKET REGISTRATION
     *
     *   // In your mod's packet registration handler (RegisterPayloadHandlersEvent):
     *
     *   @SubscribeEvent
     *   public void registerPackets(RegisterPayloadHandlersEvent event) {
     *       PayloadRegistrar registrar = event.registrar(Abysscore.MODID);
     *
     *       // S2C — server sends data to client
     *       AbyssPacketHandler.registerS2C(
     *           registrar,
     *           MyS2CPacket.TYPE,
     *           MyS2CPacket.CODEC,
     *           () -> MyS2CPacketClientHandler::handle  // Supplier — only resolved on CLIENT
     *       );
     *
     *       // C2S — client sends data to server
     *       AbyssPacketHandler.registerC2S(
     *           registrar,
     *           MyC2SPacket.TYPE,
     *           MyC2SPacket.CODEC,
     *           MyC2SPacketHandler::handle  // no Supplier needed — server always has this
     *       );
     *   }
     *
     * */

    public static <T extends CustomPacketPayload> void registerS2C(
            PayloadRegistrar registrar,
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            Supplier<IPayloadHandler<T>> clientHandler) {
        registrar.playToClient(type, codec,
                FMLEnvironment.dist == Dist.CLIENT
                        ? clientHandler.get()
                        : (pkt, ctx) -> {});
    }

    public static <T extends CustomPacketPayload> void registerC2S(
            PayloadRegistrar registrar,
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            IPayloadHandler<T> serverHandler) {
        registrar.playToServer(type, codec, serverHandler);
    }
}

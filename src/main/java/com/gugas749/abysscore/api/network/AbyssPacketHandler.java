package com.gugas749.abysscore.api.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AbyssPacketHandler {

    /*
     *           EXAMPLE PACKET REGISTRATION (Forge 1.20.1)
     *
     *   public class ModPackets {
     *       public static final AbyssNetworkChannel CHANNEL =
     *           AbyssNetworkChannel.create(new ResourceLocation("mymod", "main"), "1");
     *
     *       // Call ONCE from the mod constructor
     *       public static void register() {
     *           // S2C — server sends data to client
     *           AbyssPacketHandler.registerS2C(
     *               CHANNEL,
     *               MyS2CPacket.class,
     *               MyS2CPacket.CODEC,
     *               () -> MyS2CPacketClientHandler::handle  // Supplier — only resolved on CLIENT
     *           );
     *
     *           // C2S — client sends data to server
     *           AbyssPacketHandler.registerC2S(
     *               CHANNEL,
     *               MyC2SPacket.class,
     *               MyC2SPacket.CODEC,
     *               MyC2SPacketHandler::handle  // no Supplier needed — server always has this
     *           );
     *       }
     *   }
     *
     *   // Sending:
     *   ModPackets.CHANNEL.sendToPlayer(player, new MyS2CPacket(...));
     *   ModPackets.CHANNEL.sendToServer(new MyC2SPacket(...));
     *
     * */

    /** Same shape as NeoForge's IPayloadHandler: (packet, context) -> void. */
    @FunctionalInterface
    public interface Handler<T> {
        void handle(T packet, AbyssPacketContext ctx);
    }

    public static <T> void registerS2C(
            AbyssNetworkChannel channel,
            Class<T> type,
            AbyssPacketCodec<T> codec,
            Supplier<Handler<T>> clientHandler) {

        // On a dedicated server the client handler class does not exist, so we never call
        // clientHandler.get() there. The packet is still registered (the index must exist
        // on both sides), it just gets a do-nothing handler that is never used.
        Handler<T> handler = FMLEnvironment.dist == Dist.CLIENT
                ? clientHandler.get()
                : (pkt, ctx) -> {};

        register(channel, type, codec, handler, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static <T> void registerC2S(
            AbyssNetworkChannel channel,
            Class<T> type,
            AbyssPacketCodec<T> codec,
            Handler<T> serverHandler) {

        register(channel, type, codec, serverHandler, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <T> void register(
            AbyssNetworkChannel channel,
            Class<T> type,
            AbyssPacketCodec<T> codec,
            Handler<T> handler,
            NetworkDirection direction) {

        channel.raw().messageBuilder(type, channel.nextId(), direction)
                // Forge's encoder is (packet, buf); our codec is (buf, packet) — swap here
                .encoder((pkt, buf) -> codec.encoder().accept(buf, pkt))
                .decoder(buf -> codec.decoder().apply(buf))
                .consumerNetworkThread((pkt, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    // Handlers call ctx.enqueueWork(...) themselves, exactly like on NeoForge
                    handler.handle(pkt, new AbyssPacketContext(ctx));
                    ctx.setPacketHandled(true);
                })
                .add();
    }
}

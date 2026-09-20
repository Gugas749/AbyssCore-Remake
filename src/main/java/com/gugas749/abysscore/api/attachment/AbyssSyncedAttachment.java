package com.gugas749.abysscore.api.attachment;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.api.network.AbyssPacketHandler;
import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AbyssSyncedAttachment<T> {

    // Client handler
    //AbyssSyncedAttachment<?> attachment = AbyssSyncedAttachment.REGISTRY.get(packet.id());
    // attachment.decode(packet.data()) gives back the value
    // then apply it to the local player

    public static final Map<ResourceLocation, AbyssSyncedAttachment<?>> REGISTRY = new HashMap<>();

    private final ResourceLocation id;
    private final Supplier<AttachmentType<T>> attachmentType;
    private final Codec<T> codec;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Abysscore.MODID);

    public AbyssSyncedAttachment(ResourceLocation id, Supplier<AttachmentType<T>> attachmentType, Codec<T> codec) {
        this.id = id;
        this.attachmentType = attachmentType;
        this.codec = codec;
    }

    public T get(ServerPlayer player) {
        return player.getData(this.attachmentType.get());
    }

    public AttachmentType<T> getAttachmentType() {
        return this.attachmentType.get();
    }

    public void set(ServerPlayer player, T value) {
        player.setData(this.attachmentType.get(), value);

        byte[] encoded = encode(value);
        SyncAttachmentPacket packet = new SyncAttachmentPacket(this.id, encoded);
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static <T> AbyssSyncedAttachment<T> register(String name, T defaultValue, Codec<T> codec) {
        var holder = ATTACHMENT_TYPES.register(name, () ->
                AttachmentType.builder(() -> defaultValue)
                        .serialize(codec)
                        .build()
        );

        ResourceLocation id = Abysscore.asResource(name);
        AbyssSyncedAttachment<T> instance = new AbyssSyncedAttachment<>(id, holder, codec);

        REGISTRY.put(id, instance);
        return instance;
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return; // only on death, not dimension change
        for (AbyssSyncedAttachment<?> attachment : REGISTRY.values()) {
            attachment.cloneData(event.getOriginal(), event.getEntity());
        }
    }

    //-----------------------------------------------------------------------------------
    //                                      HELPERS
    //-----------------------------------------------------------------------------------

    // Encode T to byte[]
    private byte[] encode(T value) {
        var nbt = codec.encodeStart(NbtOps.INSTANCE, value)
                .getOrThrow();  // throws if encoding fails

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeNbt((CompoundTag) nbt);
        return buf.array();
    }

    // Decode byte[] back to T
    public T decode(byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        var nbt = buf.readNbt();
        return codec.parse(NbtOps.INSTANCE, nbt)
                .getOrThrow();
    }

    private void cloneData(Player original, Player clone) {
        clone.setData(this.attachmentType.get(), original.getData(this.attachmentType.get()));
    }
}

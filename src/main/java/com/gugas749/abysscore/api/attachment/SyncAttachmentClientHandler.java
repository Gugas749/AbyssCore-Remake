package com.gugas749.abysscore.api.attachment;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public class SyncAttachmentClientHandler {

    public static void handle(SyncAttachmentPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            AbyssSyncedAttachment<?> attachment = AbyssSyncedAttachment.REGISTRY.get(packet.id());
            if (attachment == null) return;

            applyUnchecked(attachment, player, packet.data());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> void applyUnchecked(AbyssSyncedAttachment<T> attachment, Player player, byte[] data) {
        T value = attachment.decode(data);
        player.setData(attachment.getAttachmentType(), value);
    }
}
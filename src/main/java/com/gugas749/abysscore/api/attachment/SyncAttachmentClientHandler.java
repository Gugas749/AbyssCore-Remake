package com.gugas749.abysscore.api.attachment;

import com.gugas749.abysscore.api.network.AbyssPacketContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SyncAttachmentClientHandler {

    public static void handle(SyncAttachmentPacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> {
            AbyssSyncedAttachment<?> attachment = AbyssSyncedAttachment.REGISTRY.get(packet.id());
            if (attachment == null) return;
            attachment.applyClientValue(packet.data());
        });
    }
}

package com.gugas749.abysscore.api.permission;

import com.gugas749.abysscore.Abysscore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * CLIENT: this player's own permission level, as last told by the server.
 *
 *   if (AbyssClientPermission.has(AbyssPermissionLevel.MODERATOR)) { show staff button }
 *
 * For the USER EXPERIENCE only (hide buttons, refuse to open staff screens). A modified client
 * can fake this value, so the server must still check AbyssPermissionHandler on every action.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Abysscore.MODID, value = Dist.CLIENT)
public final class AbyssClientPermission {

    private static AbyssPermissionLevel level = AbyssPermissionLevel.PLAYER;

    private AbyssClientPermission() {}

    public static AbyssPermissionLevel get() {
        return level;
    }

    public static boolean has(AbyssPermissionLevel required) {
        return level.isAtLeast(required);
    }

    public static void handle(PermissionSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> level = packet.level());
    }

    /** Leaving a server: back to PLAYER until the next server says otherwise. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        level = AbyssPermissionLevel.PLAYER;
    }
}

package com.gugas749.abysscore.api.permission;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.api.network.AbyssPacketContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * CLIENT: this player's own permission level, as last told by the server.
 *
 *   if (AbyssClientPermission.has(AbyssPermissionLevel.MODERATOR)) { show staff button }
 *
 * For the USER EXPERIENCE only (hide buttons, refuse to open staff screens). A modified client
 * can fake this value, so the server must still check AbyssPermissionHandler on every action.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Abysscore.MODID, value = Dist.CLIENT)
public final class AbyssClientPermission {

    private static AbyssPermissionLevel level = AbyssPermissionLevel.PLAYER;

    private AbyssClientPermission() {}

    public static AbyssPermissionLevel get() {
        return level;
    }

    public static boolean has(AbyssPermissionLevel required) {
        return level.isAtLeast(required);
    }

    public static void handle(PermissionSyncPacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> level = packet.level());
    }

    /** Leaving a server: back to PLAYER until the next server says otherwise. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        level = AbyssPermissionLevel.PLAYER;
    }
}

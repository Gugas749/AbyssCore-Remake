package com.gugas749.abysscore.api.permission;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SERVER: keeps every client's AbyssClientPermission up to date.
 *
 * Instead of hooking every place a level can change (grant, revoke, /op, /deop — the last two
 * fire no event at all), it simply re-checks every online player every 5 seconds and sends
 * a packet ONLY when their level actually changed. Cheap and impossible to forget a case.
 */
@Mod.EventBusSubscriber(modid = Abysscore.MODID)
public final class AbyssPermissionSync {

    private static final int CHECK_INTERVAL_TICKS = 100;   // 5 seconds

    /** Last level we told each player. */
    private static final Map<UUID, AbyssPermissionLevel> SENT = new HashMap<>();
    private static int tickCounter = 0;

    private AbyssPermissionSync() {}

    /** The level a player effectively has (ADMIN also counts OPs, see AbyssPermissionHandler.has). */
    public static AbyssPermissionLevel levelOf(ServerPlayer player) {
        if (AbyssPermissionHandler.has(player, AbyssPermissionLevel.ADMIN)) return AbyssPermissionLevel.ADMIN;
        if (AbyssPermissionHandler.has(player, AbyssPermissionLevel.MODERATOR)) return AbyssPermissionLevel.MODERATOR;
        return AbyssPermissionLevel.PLAYER;
    }

    /** Sends the player's level if it differs from what we last sent them. */
    public static void sync(ServerPlayer player) {
        AbyssPermissionLevel level = levelOf(player);
        if (SENT.put(player.getUUID(), level) != level) {
            PacketHandler.CHANNEL.sendToPlayer(player, new PermissionSyncPacket(level));
        }
    }

    /**
     * LOWEST priority: AbyssPermissionHandler also reacts to login (it auto-grants ADMIN to OPs).
     * Running last means we send the level AFTER that has happened.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SENT.remove(player.getUUID());   // fresh client → always send
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            sync(player);
        }
    }
}

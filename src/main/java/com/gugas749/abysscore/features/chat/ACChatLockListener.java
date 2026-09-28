package com.gugas749.abysscore.features.chat;

import com.gugas749.abysscore.AbysscoreServerConfig;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ACChatLockListener {

    private static final Set<String> BLOCKED_COMMANDS = Set.of(
        "say", "me"
    );

    private static final Set<UUID> mutedAdmins = new HashSet<>();

    public static void toggleAdminMute(UUID uuid) {
        if (mutedAdmins.contains(uuid)) mutedAdmins.remove(uuid);
        else mutedAdmins.add(uuid);
    }

    public static boolean isAdminMuted(UUID uuid) {
        return mutedAdmins.contains(uuid);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChat(ServerChatEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (AbyssPermissionHandler.has(player, AbyssPermissionLevel.MODERATOR)) {
                if (isAdminMuted(player.getUUID())) event.setCanceled(true);
            } else {
                if (AbysscoreServerConfig.isChatLockEnabled()){
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onCommand(CommandEvent event) {
        var source = event.getParseResults().getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        if (player.hasPermissions(2)) return;

        String input = event.getParseResults().getReader().getString().trim();
        if (input.startsWith("/")) input = input.substring(1);
        String commandName = input.split(" ")[0].toLowerCase();

        if (BLOCKED_COMMANDS.contains(commandName)) {
            if (AbysscoreServerConfig.isChatLockEnabled()) {
                event.setCanceled(true);
            }
        }
    }
}

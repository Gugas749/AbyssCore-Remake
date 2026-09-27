package com.gugas749.abysscore.commands.subRegisters;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.TickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ACGodCommands {

    private static final Set<UUID> godModePlayers = new HashSet<>();

    public static Set<UUID> godModePlayers() { return godModePlayers; }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("abysscore")
                .requires(source -> AbyssPermissionHandler.sourceHas(source, AbyssPermissionLevel.MODERATOR))

                .then(Commands.literal("god")

                    .executes(ACGodCommands::executeToggleSelf)

                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ACGodCommands::executeToggleTarget)
                    )
                )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore god [player]");
    }

    private static int executeToggleSelf(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.god.player_only"));
            return 0;
        }
        return toggle(source, player);
    }

    private static int executeToggleTarget(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        try {
            return toggle(source, EntityArgument.getPlayer(ctx, "target"));
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.abysscore.god.player_not_found"));
            return 0;
        }
    }

    private static int toggle(CommandSourceStack source, ServerPlayer player) {
        boolean nowGod;

        if (godModePlayers.contains(player.getUUID())) {
            godModePlayers.remove(player.getUUID());
            nowGod = false;
        } else {
            godModePlayers.add(player.getUUID());
            nowGod = true;
        }

        source.sendSuccess(
            () -> Component.translatable(
                nowGod ? "message.abysscore.god.enabled" : "message.abysscore.god.disabled",
                player.getName().getString()
            ),
            true
        );

        if (source.getEntity() != player) {
            player.sendSystemMessage(Component.translatable(
                nowGod ? "message.abysscore.god.enabled_self" : "message.abysscore.god.disabled_self"
            ));
        }

        Abysscore.LOGGER.info("[AbyssCore] God mode {} for {}",
            nowGod ? "ENABLED" : "DISABLED", player.getName().getString());
        return 1;
    }

    // ── Damage cancel ─────────────────────────────────────────────────────────

    // Forge 1.20.1 has no LivingIncomingDamageEvent. LivingAttackEvent fires at the start of
    // LivingEntity#hurt and is cancelable, which blocks the damage (and the hurt animation/knockback).
    @SubscribeEvent
    public void onIncomingDamage(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (godModePlayers.contains(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Forge fires START and END each tick; NeoForge's PlayerTickEvent.Post == END
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!godModePlayers.contains(player.getUUID())) return;
        if (player.tickCount % 20 != 0) return; // once per second

        // Clear any mob that is targeting this god mode player
        player.serverLevel().getEntitiesOfClass(
                net.minecraft.world.entity.Mob.class,
                player.getBoundingBox().inflate(64),
                mob -> player.equals(mob.getTarget())
        ).forEach(mob -> mob.setTarget(null));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static boolean hasGodMode(UUID uuid) {
        return godModePlayers.contains(uuid);
    }

    public static void onPlayerLeave(UUID uuid) {
        godModePlayers.remove(uuid);
    }
}

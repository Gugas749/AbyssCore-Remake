package com.gugas749.abysscore.commands;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.commands.subRegisters.*;
import com.gugas749.abysscore.network.menu.MenuPacketHandlers;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class ACModCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        Abysscore.LOGGER.info("[AbyssCore] Registering commands...");

        register(event.getDispatcher());

        ACGodCommands.register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return 0;
                            PacketDistributor.sendToPlayer(player,
                                    MenuPacketHandlers.buildMenuPacket(player));
                            return 1;
                        })
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore");
    }

}

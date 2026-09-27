package com.gugas749.abysscore;

import com.gugas749.abysscore.api.attachment.AbyssSyncedAttachment;
import com.gugas749.abysscore.api.effects.AbyssEffectHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.client.ClientSetup;
import com.gugas749.abysscore.commands.ACModCommands;
import com.gugas749.abysscore.commands.subRegisters.ACGodCommands;
import com.gugas749.abysscore.features.blind.ACBlindManager;
import com.gugas749.abysscore.features.bulk.BulkCommandManager;
import com.gugas749.abysscore.features.chat.ACChatLockListener;
import com.gugas749.abysscore.features.dimen.ACDimensionManager;
import com.gugas749.abysscore.features.regions.ACBlockProtectionListener;
import com.gugas749.abysscore.features.regions.ACNoEntryListener;
import com.gugas749.abysscore.features.title.ACTitleManager;
import com.gugas749.abysscore.features.vanish.ACVanishExtras;
import com.gugas749.abysscore.features.vanish.ACVanishStateListener;
import com.gugas749.abysscore.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(Abysscore.MODID)
public class Abysscore {
    public static final String MODID = "abysscore";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Forge 1.20.1: no constructor parameters. The mod event bus comes from the loading context.
    public Abysscore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ── API ──────────────────────────────────────────────────────────────
        MinecraftForge.EVENT_BUS.register(new AbyssEffectHandler());
        MinecraftForge.EVENT_BUS.register(AbyssSyncedAttachment.class);   // static handlers
        MinecraftForge.EVENT_BUS.register(AbyssPermissionHandler.class);  // static handlers

        // ── Network ──────────────────────────────────────────────────────────
        // SimpleChannel packets are registered directly, no event needed
        PacketHandler.register();

        // ── Client only ──────────────────────────────────────────────────────
        // Everything client-side lives in ClientSetup so this class never touches client classes
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modEventBus);
        }

        // ── Server ───────────────────────────────────────────────────────────
        MinecraftForge.EVENT_BUS.register(new ACModCommands());
        MinecraftForge.EVENT_BUS.register(new ACGodCommands());
        MinecraftForge.EVENT_BUS.register(new ACBlockProtectionListener());
        MinecraftForge.EVENT_BUS.register(new ACVanishStateListener());
        MinecraftForge.EVENT_BUS.register(new ACNoEntryListener());
        MinecraftForge.EVENT_BUS.register(new ACChatLockListener());
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeave);

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> LOGGER.info("[AbyssCore] Loading..."));
    }

    public static ResourceLocation asResource(String path) {
        // 1.20.1 has no ResourceLocation.fromNamespaceAndPath — use the constructor
        return new ResourceLocation(MODID, path);
    }

    private void onServerStarting(ServerStartingEvent event) {
        AbysscoreServerConfig.load();

        BulkCommandManager.load();
        ACDimensionManager.load();   // load registry
        ACDimensionManager.onServerStarted(event.getServer());  // cleanup pending states
        ACTitleManager.load();
        AbyssPermissionHandler.load();
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ACBlindManager.onPlayerJoin(player);
    }

    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ACVanishExtras.onPlayerLeave(player.getUUID());
        ACGodCommands.onPlayerLeave(player.getUUID());
        ACNoEntryListener.onPlayerLeave(player.getUUID());
    }
}

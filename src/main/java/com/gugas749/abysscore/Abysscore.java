package com.gugas749.abysscore;

import com.gugas749.abysscore.api.attachment.AbyssSyncedAttachment;
import com.gugas749.abysscore.api.attachment.SyncAttachmentClientHandler;
import com.gugas749.abysscore.api.attachment.SyncAttachmentPacket;
import com.gugas749.abysscore.api.effects.AbyssEffectHandler;
import com.gugas749.abysscore.api.network.AbyssPacketHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.client.ACVanishHudHandler;
import com.gugas749.abysscore.client.ClientTickHandler;
import com.gugas749.abysscore.client.KeyBindings;
import com.gugas749.abysscore.client.ui.screens.BlindScreen;
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
import com.gugas749.abysscore.network.ClientPacketHandler;
import com.gugas749.abysscore.network.PacketHandler;
import com.gugas749.abysscore.network.region.NoEntryHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(Abysscore.MODID)
public class Abysscore {
    public static final String MODID = "abysscore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Abysscore(IEventBus modEventBus, ModContainer modContainer) {

        // ── API ──────────────────────────────────────────────────────────────
        NeoForge.EVENT_BUS.register(new AbyssEffectHandler());
        AbyssSyncedAttachment.ATTACHMENT_TYPES.register(modEventBus);
        NeoForge.EVENT_BUS.register(AbyssSyncedAttachment.class);
        NeoForge.EVENT_BUS.register(AbyssPermissionHandler.class);

        // ── Network ──────────────────────────────────────────────────────────
        modEventBus.addListener(PacketHandler::registerPayloads);

        // ── Client only ──────────────────────────────────────────────────────
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(KeyBindings::register);
            NeoForge.EVENT_BUS.register(new ClientTickHandler());
            NeoForge.EVENT_BUS.register(new ACVanishHudHandler());
            NeoForge.EVENT_BUS.register(new NoEntryHandler());
            NeoForge.EVENT_BUS.register(new BlindScreen());
        }

        // ── Server ───────────────────────────────────────────────────────────
        NeoForge.EVENT_BUS.register(new ACModCommands());
        NeoForge.EVENT_BUS.register(new ACGodCommands());
        NeoForge.EVENT_BUS.register(new ACBlockProtectionListener());
        NeoForge.EVENT_BUS.register(new ACVanishStateListener());
        NeoForge.EVENT_BUS.register(new ACNoEntryListener());
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLeave);
        NeoForge.EVENT_BUS.register(new ACChatLockListener());

        modEventBus.addListener(this::commonSetup);
        //modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> LOGGER.info("[AbyssCore] Loading..."));
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void onServerStarting(ServerStartingEvent event) {
        BulkCommandManager.load();
        ACDimensionManager.load();   // load registry
        ACDimensionManager.onServerStarted(event.getServer());  // cleanup pending states
        ACTitleManager.load();
        AbyssPermissionHandler.load();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ACBlindManager.onPlayerJoin(player);
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ACVanishExtras.onPlayerLeave(player.getUUID());
        ACGodCommands.onPlayerLeave(player.getUUID());
        ACNoEntryListener.onPlayerLeave(player.getUUID());
    }
}

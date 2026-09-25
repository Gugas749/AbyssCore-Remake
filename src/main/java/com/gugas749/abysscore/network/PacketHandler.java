package com.gugas749.abysscore.network;

import com.gugas749.abysscore.api.attachment.SyncAttachmentClientHandler;
import com.gugas749.abysscore.api.attachment.SyncAttachmentPacket;
import com.gugas749.abysscore.api.network.AbyssPacketHandler;
import com.gugas749.abysscore.client.ACVanishHudHandler;
import com.gugas749.abysscore.client.ui.AbyssCoreMenuScreen;
import com.gugas749.abysscore.client.ui.screens.*;
import com.gugas749.abysscore.features.regions.ACNoEntryListener;
import com.gugas749.abysscore.network.binds.KeyPressHandler;
import com.gugas749.abysscore.network.binds.KeyPressPacket;
import com.gugas749.abysscore.network.blind.BlindSyncPacket;
import com.gugas749.abysscore.network.bulk.SubmitBulkCommandHandler;
import com.gugas749.abysscore.network.bulk.SubmitBulkCommandPacket;
import com.gugas749.abysscore.network.dimen.DimenPacketHandlers;
import com.gugas749.abysscore.network.dimen.OpenDimenCreateScreenPacket;
import com.gugas749.abysscore.network.dimen.SubmitDimenCreatePacket;
import com.gugas749.abysscore.network.menu.MenuPacketHandlers;
import com.gugas749.abysscore.network.menu.packets.*;
import com.gugas749.abysscore.network.menu.packets.OpenMainMenuPacket;
import com.gugas749.abysscore.network.region.*;
import com.gugas749.abysscore.network.vanish.VanishStateSyncPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");

        // ── S2C ──────────────────────────────────────────────────────────────
        
        AbyssPacketHandler.registerS2C(registrar,
            VanishStateSyncPacket.TYPE, VanishStateSyncPacket.CODEC,
            () -> ACVanishHudHandler::handleSync);

        AbyssPacketHandler.registerS2C(registrar,
            NoEntryPacket.TYPE, NoEntryPacket.CODEC,
            () -> NoEntryHandler::handlePacket);

        AbyssPacketHandler.registerS2C(registrar,
            BlindSyncPacket.TYPE, BlindSyncPacket.CODEC,
            () -> BlindScreen::handleSync);

        AbyssPacketHandler.registerS2C(
                registrar,
                SyncAttachmentPacket.TYPE,
                SyncAttachmentPacket.CODEC,
                () -> SyncAttachmentClientHandler::handle
        );

        AbyssPacketHandler.registerS2C(registrar,
                OpenDimenCreateScreenPacket.TYPE, OpenDimenCreateScreenPacket.CODEC,
                () -> ClientPacketHandler::handleOpenDimenCreate);

        AbyssPacketHandler.registerS2C(registrar,
                OpenRegionScreenPacket.TYPE, OpenRegionScreenPacket.CODEC,
                () -> ClientPacketHandler::handleOpenRegionScreen);

        AbyssPacketHandler.registerS2C(registrar,
                OpenMainMenuPacket.TYPE, OpenMainMenuPacket.CODEC,
                () -> ClientPacketHandler::handleOpenMainMenu);

        // ── C2S ──────────────────────────────────────────────────────────────


        AbyssPacketHandler.registerC2S(registrar,
                KeyPressPacket.TYPE, KeyPressPacket.CODEC,
                KeyPressHandler::handle);

        AbyssPacketHandler.registerC2S(registrar,
                SubmitBulkCommandPacket.TYPE, SubmitBulkCommandPacket.CODEC,
                SubmitBulkCommandHandler::handle);

        AbyssPacketHandler.registerC2S(registrar,
                SubmitDimenCreatePacket.TYPE, SubmitDimenCreatePacket.CODEC,
                DimenPacketHandlers::handleCreate);

        AbyssPacketHandler.registerC2S(registrar,
                SubmitRegionUpdatePacket.TYPE, SubmitRegionUpdatePacket.CODEC,
                RegionScreenPacketHandlers::handleRegionUpdate);

        AbyssPacketHandler.registerC2S(registrar,
                ReadyToTeleportPacket.TYPE, ReadyToTeleportPacket.CODEC,
                ACNoEntryListener::handleReadyToTeleport);

        AbyssPacketHandler.registerC2S(registrar,
                MenuActionPacket.TYPE, MenuActionPacket.CODEC,
                MenuPacketHandlers::handleAction);

        AbyssPacketHandler.registerC2S(registrar,
                SaveTitlePacket.TYPE, SaveTitlePacket.CODEC,
                MenuPacketHandlers::handleSaveTitle);

        AbyssPacketHandler.registerC2S(registrar,
                RequestRegionScreenPacket.TYPE, RequestRegionScreenPacket.CODEC,
                RegionScreenPacketHandlers::handleRequest);
    }
}

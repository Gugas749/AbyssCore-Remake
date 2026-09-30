package com.gugas749.abysscore.network;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.api.attachment.SyncAttachmentClientHandler;
import com.gugas749.abysscore.api.attachment.SyncAttachmentPacket;
import com.gugas749.abysscore.api.network.AbyssNetworkChannel;
import com.gugas749.abysscore.api.network.AbyssPacketHandler;
import com.gugas749.abysscore.api.permission.AbyssClientPermission;
import com.gugas749.abysscore.api.permission.PermissionSyncPacket;
import com.gugas749.abysscore.client.ACVanishHudHandler;
import com.gugas749.abysscore.client.ui.screens.BlindScreen;
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
import com.gugas749.abysscore.network.region.*;
import com.gugas749.abysscore.network.vanish.VanishStateSyncPacket;

public class PacketHandler {

    /** AbyssCore's own channel. Bump the version string whenever a packet's fields change. */
    public static final AbyssNetworkChannel CHANNEL =
            AbyssNetworkChannel.create(Abysscore.asResource("main"), "3");   // 3: permission sync for the GUI API

    /**
     * Called once from the mod constructor.
     * ORDER MATTERS: each registration gets the next index (0, 1, 2...), and client and
     * server must agree on it. Always add new packets at the END of this method.
     */
    public static void register() {

        // ── S2C ──────────────────────────────────────────────────────────────

        AbyssPacketHandler.registerS2C(CHANNEL,
            VanishStateSyncPacket.class, VanishStateSyncPacket.CODEC,
            () -> ACVanishHudHandler::handleSync);

        AbyssPacketHandler.registerS2C(CHANNEL,
            NoEntryPacket.class, NoEntryPacket.CODEC,
            () -> NoEntryHandler::handlePacket);

        AbyssPacketHandler.registerS2C(CHANNEL,
            BlindSyncPacket.class, BlindSyncPacket.CODEC,
            () -> BlindScreen::handleSync);

        AbyssPacketHandler.registerS2C(CHANNEL,
            SyncAttachmentPacket.class, SyncAttachmentPacket.CODEC,
            () -> SyncAttachmentClientHandler::handle);

        AbyssPacketHandler.registerS2C(CHANNEL,
            OpenDimenCreateScreenPacket.class, OpenDimenCreateScreenPacket.CODEC,
            () -> ClientPacketHandler::handleOpenDimenCreate);

        AbyssPacketHandler.registerS2C(CHANNEL,
            OpenRegionScreenPacket.class, OpenRegionScreenPacket.CODEC,
            () -> ClientPacketHandler::handleOpenRegionScreen);

        AbyssPacketHandler.registerS2C(CHANNEL,
            OpenMainMenuPacket.class, OpenMainMenuPacket.CODEC,
            () -> ClientPacketHandler::handleOpenMainMenu);

        // ── C2S ──────────────────────────────────────────────────────────────

        AbyssPacketHandler.registerC2S(CHANNEL,
            KeyPressPacket.class, KeyPressPacket.CODEC,
            KeyPressHandler::handle);

        AbyssPacketHandler.registerC2S(CHANNEL,
            SubmitBulkCommandPacket.class, SubmitBulkCommandPacket.CODEC,
            SubmitBulkCommandHandler::handle);

        AbyssPacketHandler.registerC2S(CHANNEL,
            SubmitDimenCreatePacket.class, SubmitDimenCreatePacket.CODEC,
            DimenPacketHandlers::handleCreate);

        AbyssPacketHandler.registerC2S(CHANNEL,
            SubmitRegionUpdatePacket.class, SubmitRegionUpdatePacket.CODEC,
            RegionScreenPacketHandlers::handleRegionUpdate);

        AbyssPacketHandler.registerC2S(CHANNEL,
            ReadyToTeleportPacket.class, ReadyToTeleportPacket.CODEC,
            ACNoEntryListener::handleReadyToTeleport);

        AbyssPacketHandler.registerC2S(CHANNEL,
            MenuActionPacket.class, MenuActionPacket.CODEC,
            MenuPacketHandlers::handleAction);

        AbyssPacketHandler.registerC2S(CHANNEL,
            SaveTitlePacket.class, SaveTitlePacket.CODEC,
            MenuPacketHandlers::handleSaveTitle);

        AbyssPacketHandler.registerC2S(CHANNEL,
            RequestRegionScreenPacket.class, RequestRegionScreenPacket.CODEC,
            RegionScreenPacketHandlers::handleRequest);

        // ── GUI API (added at the END — never insert packets in the middle) ──
        AbyssPacketHandler.registerS2C(CHANNEL,
            PermissionSyncPacket.class, PermissionSyncPacket.CODEC,
            () -> AbyssClientPermission::handle);
    }
}

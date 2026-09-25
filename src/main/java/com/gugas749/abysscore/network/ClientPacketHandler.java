package com.gugas749.abysscore.network;

import com.gugas749.abysscore.api.network.AbyssPacketHandler;
import com.gugas749.abysscore.client.ui.AbyssCoreMenuScreen;
import com.gugas749.abysscore.client.ui.screens.DimenCreateScreen;
import com.gugas749.abysscore.client.ui.screens.RegionManagerScreen;
import com.gugas749.abysscore.features.regions.ACNoEntryListener;
import com.gugas749.abysscore.network.binds.KeyPressHandler;
import com.gugas749.abysscore.network.binds.KeyPressPacket;
import com.gugas749.abysscore.network.bulk.SubmitBulkCommandHandler;
import com.gugas749.abysscore.network.bulk.SubmitBulkCommandPacket;
import com.gugas749.abysscore.network.dimen.DimenPacketHandlers;
import com.gugas749.abysscore.network.dimen.OpenDimenCreateScreenPacket;
import com.gugas749.abysscore.network.dimen.SubmitDimenCreatePacket;
import com.gugas749.abysscore.network.menu.MenuPacketHandlers;
import com.gugas749.abysscore.network.menu.packets.MenuActionPacket;
import com.gugas749.abysscore.network.menu.packets.OpenMainMenuPacket;
import com.gugas749.abysscore.network.menu.packets.RequestRegionScreenPacket;
import com.gugas749.abysscore.network.menu.packets.SaveTitlePacket;
import com.gugas749.abysscore.network.region.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void handleOpenDimenCreate(OpenDimenCreateScreenPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new DimenCreateScreen()));
    }

    public static void handleOpenRegionScreen(OpenRegionScreenPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new RegionManagerScreen(pkt.regions())));
    }

    public static void handleOpenMainMenu(OpenMainMenuPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new AbyssCoreMenuScreen(pkt)));
    }
}

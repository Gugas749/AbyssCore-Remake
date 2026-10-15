package com.gugas749.abysscore.features.regions;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ABY-84 — "keep_inventory" region tag.
 *
 * A player who dies inside a region with this tag keeps their inventory and XP,
 * whatever the world's keepInventory gamerule says.
 *
 * How it works (three events, in the order Minecraft fires them):
 *
 *  1. LivingDeathEvent      — the player is dying but nothing has dropped yet.
 *                             If they're in a keep_inventory region we SAVE a copy of the
 *                             inventory to NBT and EMPTY the real one, so there is nothing to drop.
 *  2. LivingExperienceDropEvent — fires next; we cancel it so no XP orbs spawn.
 *  3. PlayerEvent.Clone     — on respawn Minecraft creates a NEW ServerPlayer.
 *                             We load the saved inventory into it and copy the XP across.
 *
 * Why save/clear instead of just cancelling the drops? Cancelling LivingDropsEvent gives you a
 * flat list of item entities — you lose which slot each item was in (armor, offhand, hotbar).
 * Saving the Inventory to NBT keeps every slot exactly where it was.
 */
public class ACKeepInventoryListener {

    /** Inventories waiting for their owner to respawn. */
    private static final Map<UUID, ListTag> PENDING = new HashMap<>();

    /**
     * LOWEST priority + no receiveCanceled: we only act if NO other mod cancelled the death.
     * (PlayerRevive, for example, cancels the death to put the player in a "downed" state —
     * in that case the player isn't really dying, so we must not touch their inventory.)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        // World gamerule already keeps everything → vanilla handles it, nothing to do.
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        String dimension = level.dimension().location().toString();
        if (!ACRegionSavedData.get(level).isRestrictedAt(
                dimension, player.blockPosition(), ACBlockProtectionListener.KEEP_INVENTORY_TAG)) return;

        PENDING.put(player.getUUID(), player.getInventory().save(new ListTag()));
        player.getInventory().clearContent();   // nothing left for the death to drop

        Abysscore.LOGGER.debug("[AbyssCore] {} died in a keep_inventory region — inventory kept.",
                player.getName().getString());
    }

    /** No XP orbs for players whose inventory we're keeping (the XP is copied in onClone). */
    @SubscribeEvent
    public void onExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PENDING.containsKey(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;   // dimension change: not a death
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;

        ListTag saved = PENDING.remove(newPlayer.getUUID());
        if (saved == null) return;          // didn't die in a keep_inventory region

        newPlayer.getInventory().load(saved);

        // XP stays on the old player object after death — copy it over.
        // setExperienceLevels() also forces the XP bar to resync to the client.
        var old = event.getOriginal();
        newPlayer.setExperienceLevels(old.experienceLevel);
        newPlayer.experienceProgress = old.experienceProgress;
        newPlayer.totalExperience    = old.totalExperience;
    }
}

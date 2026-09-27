package com.gugas749.abysscore.api.attachment;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.network.PacketHandler;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Forge 1.20.1 version of the synced attachment helper.
 *
 * NeoForge has "data attachments"; Forge 1.20.1 does not. Instead, every entity has a
 * Forge "persistent data" CompoundTag ({@code player.getPersistentData()}) that is saved
 * to disk together with the player. Each attachment stores its value there, encoded with
 * its Codec, under:  persistentData / "abysscore_attachments" / "<namespace>:<name>"
 *
 * USAGE (same idea as before):
 *
 *   public static final AbyssSyncedAttachment<Integer> MANA =
 *       AbyssSyncedAttachment.register("mymod", "mana", 0, Codec.INT);
 *
 *   MANA.get(serverPlayer);        // server: read
 *   MANA.set(serverPlayer, 50);    // server: write + auto-sync to that client
 *   MANA.getClient();              // client: last value the server synced
 *
 * Differences from the NeoForge version:
 *   - No DeferredRegister anymore → consuming mods must NOT call ATTACHMENT_TYPES.register(...)
 *   - On the client use getClient() instead of player.getData(type)
 *   - Values are re-synced automatically when the player logs in
 */
public class AbyssSyncedAttachment<T> {

    private static final String ROOT_KEY = "abysscore_attachments";
    private static final String VALUE_KEY = "v";

    public static final Map<ResourceLocation, AbyssSyncedAttachment<?>> REGISTRY = new HashMap<>();

    private final ResourceLocation id;
    private final T defaultValue;
    private final Codec<T> codec;

    // Only written on the client, by SyncAttachmentClientHandler
    private volatile T clientValue;

    private AbyssSyncedAttachment(ResourceLocation id, T defaultValue, Codec<T> codec) {
        this.id = id;
        this.defaultValue = defaultValue;
        this.codec = codec;
        this.clientValue = defaultValue;
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /** Registers an attachment under the abysscore namespace (kept for compatibility). */
    public static <T> AbyssSyncedAttachment<T> register(String name, T defaultValue, Codec<T> codec) {
        return register(Abysscore.MODID, name, defaultValue, codec);
    }

    /** Registers an attachment under YOUR mod's namespace — prefer this in consuming mods. */
    public static <T> AbyssSyncedAttachment<T> register(String modId, String name, T defaultValue, Codec<T> codec) {
        ResourceLocation id = new ResourceLocation(modId, name);
        if (REGISTRY.containsKey(id)) {
            throw new IllegalStateException("Duplicate AbyssSyncedAttachment id: " + id);
        }
        AbyssSyncedAttachment<T> instance = new AbyssSyncedAttachment<>(id, defaultValue, codec);
        REGISTRY.put(id, instance);
        return instance;
    }

    // ── Server API ────────────────────────────────────────────────────────────

    public ResourceLocation id() {
        return id;
    }

    public T get(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_KEY);
        if (!root.contains(id.toString())) return defaultValue;
        return decode(root.getCompound(id.toString()));
    }

    public void set(ServerPlayer player, T value) {
        writeRaw(player, value);
        sync(player);
    }

    /** Sends the current server value to that player's client. */
    public void sync(ServerPlayer player) {
        PacketHandler.CHANNEL.sendToPlayer(player, new SyncAttachmentPacket(id, encode(get(player))));
    }

    // ── Client API ────────────────────────────────────────────────────────────

    /** Last value received from the server (default until the first sync). */
    public T getClient() {
        return clientValue;
    }

    void applyClientValue(CompoundTag data) {
        this.clientValue = decode(data);
    }

    // ── Events ────────────────────────────────────────────────────────────────

    /**
     * On respawn (and when coming back from the End) Minecraft creates a NEW player object.
     * Copy every attachment from the old object to the new one.
     * The NeoForge version only did this on death; returning from the End also needs it here.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        for (AbyssSyncedAttachment<?> attachment : REGISTRY.values()) {
            attachment.copy(event.getOriginal(), event.getEntity());
        }
    }

    /** Client caches start empty on every login, so push all values once. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (AbyssSyncedAttachment<?> attachment : REGISTRY.values()) {
            attachment.sync(player);
        }
    }

    //-----------------------------------------------------------------------------------
    //                                      HELPERS
    //-----------------------------------------------------------------------------------

    private void writeRaw(Player player, T value) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag root = persistent.getCompound(ROOT_KEY); // returns a new empty tag if missing
        root.put(id.toString(), encode(value));
        persistent.put(ROOT_KEY, root);
    }

    private void copy(Player original, Player clone) {
        CompoundTag oldRoot = original.getPersistentData().getCompound(ROOT_KEY);
        if (!oldRoot.contains(id.toString())) return;

        CompoundTag newPersistent = clone.getPersistentData();
        CompoundTag newRoot = newPersistent.getCompound(ROOT_KEY);
        newRoot.put(id.toString(), oldRoot.getCompound(id.toString()).copy());
        newPersistent.put(ROOT_KEY, newRoot);
    }

    // T → {"v": <encoded tag>}. Wrapped in a CompoundTag because a codec can produce
    // any Tag type (IntTag, StringTag, ...) and we want one consistent shape.
    private CompoundTag encode(T value) {
        Tag encoded = codec.encodeStart(NbtOps.INSTANCE, value)
                // 1.20.1 DataFixerUpper: getOrThrow(allowPartial, errorConsumer)
                .getOrThrow(false, err -> Abysscore.LOGGER.error("[AbyssCore] Failed to encode attachment {}: {}", id, err));
        CompoundTag wrapper = new CompoundTag();
        wrapper.put(VALUE_KEY, encoded);
        return wrapper;
    }

    private T decode(CompoundTag wrapper) {
        Tag raw = wrapper.get(VALUE_KEY);
        if (raw == null) return defaultValue;
        return codec.parse(NbtOps.INSTANCE, raw)
                .resultOrPartial(err -> Abysscore.LOGGER.error("[AbyssCore] Failed to decode attachment {}: {}", id, err))
                .orElse(defaultValue);
    }
}

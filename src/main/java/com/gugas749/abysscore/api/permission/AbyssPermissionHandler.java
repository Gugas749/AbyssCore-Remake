package com.gugas749.abysscore.api.permission;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.features.title.ACTitle;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class AbyssPermissionHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE =
            FMLPaths.CONFIGDIR.get().resolve("abysscore_permissions.json");

    private static final Map<UUID, AbyssPermissionLevel> permissions = new HashMap<>();

    public static void grant(UUID uuid, AbyssPermissionLevel level) {
        permissions.put(uuid, level);
        save();
    }
    public static void revoke(UUID uuid) {
        permissions.put(uuid, AbyssPermissionLevel.PLAYER);
        save();
    }
    public static boolean has(ServerPlayer player, AbyssPermissionLevel required) {
        if (required == AbyssPermissionLevel.ADMIN) {
            return player.hasPermissions(2);
        }

        AbyssPermissionLevel level = permissions.getOrDefault(
                player.getUUID(), AbyssPermissionLevel.PLAYER);
        return level.isAtLeast(required);
    }

    public static boolean sourceHas(CommandSourceStack source, AbyssPermissionLevel required) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return false;
        return has(player, required);
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    public static void load() {
        permissions.clear();
        if (!Files.exists(CONFIG_FILE)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            // Use JsonObject not JsonArray — it's a key/value map
            var obj = GSON.fromJson(reader, com.google.gson.JsonObject.class);
            if (obj != null) obj.entrySet().forEach(e -> {
                UUID uuid = UUID.fromString(e.getKey());
                AbyssPermissionLevel level = AbyssPermissionLevel.valueOf(
                        e.getValue().getAsString()); // "MODERATOR" → enum constant
                permissions.put(uuid, level);
            });
            Abysscore.LOGGER.info("[AbyssCore] Loaded {} permission(s).", permissions.size());
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to load permissions: {}", e.getMessage());
        }
    }

    public static void save() {
        var obj = new com.google.gson.JsonObject();
        // UUID.toString() → "MODERATOR" string
        permissions.forEach((uuid, level) -> obj.addProperty(uuid.toString(), level.name()));
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(obj, writer);
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to save permissions: {}", e.getMessage());
        }
    }
}

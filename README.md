<p align="center"><img src="./.idea/icon.png" alt="Logo" width="300"></p>
<h1 align="center">AbyssCore  <br>
	<a><img src="https://img.shields.io/badge/Loader-Forge-1976d2?style=flat-square&color=orange&logo=curseforge&labelColor=0d1117" alt="ModLoader"></a>
	<br><br>
</h1>
<br>

<p>Core utility mod for modpacks.</p>
<p>AbyssCore adds player restriction tags, saved regions, vanish, reusable bulk commands, command key binds, and an API for damages, effects and others.</p>

---

## Contents

- [Installation](#installation)
- [Permissions](#permissions)
- [The staff menu](#the-staff-menu)
- [Commands](#commands)
- [Regions](#regions)
- [Bulk commands and key binds](#bulk-commands-and-key-binds)
- [Help requests](#help-requests)
- [Chat lock](#chat-lock)
- [Corruption](#corruption)
- [Configuration files](#configuration-files)
- [For developers: the API](#for-developers-the-api)

---

## Installation

| | |
|---|---|
| Minecraft | 1.20.1 |
| Loader | Forge 47.3+ |
| Required | [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) |
| Side | Install on **both** the server and every client (it adds screens, a HUD and keybinds) |

Optional integrations, used automatically when present:

- **Vanishmod**: vanish toggles and per-player visibility exceptions
- **WorldEdit**: create regions from your current selection
- **Waystones**: blocked inside `no_tp` regions

---

## Permissions

AbyssCore has its own three permission levels, shared with every Abyss mod that uses it:

| Level | Who | Can use |
|---|---|---|
| **ADMIN** | Every server OP (granted automatically on join, removed if they lose OP) | Everything |
| **MODERATOR** | Players you grant it to, they do **not** need OP | The staff menu, god, blind, titles, help requests, bulk commands |
| **PLAYER** | Everyone else | `/abysshelp`, `/abysscore run` (if the bulk command allows it) |

Higher levels include the lower ones (an ADMIN can do anything a MODERATOR can).

```
/abysscore permission grant <player> MODERATOR
/abysscore permission revoke <player>
```

Saved in `config/abysscore/abysscore_permissions.json`.

---

## The staff menu

`/abysscore` (MODERATOR) opens the staff menu. Each tab is a list: select a row, then use the buttons under it.

| Tab | What it does | Level |
|---|---|---|
| Players | Toggle vanish, god mode and blind for any online player | MODERATOR (vanish: ADMIN) |
| Titles | Create, edit and delete saved titles, and send them to everyone, a scoreboard tag or a team | MODERATOR |
| Vanish | Team visibility, and show or hide a vanished player to specific people | ADMIN |
| Regions | Opens the Region Manager | ADMIN |
| Dimensions | Create custom dimensions and teleport to them | ADMIN |
| Help | Pending `/abysshelp` requests, accept one to teleport to the player | MODERATOR |
| Bulk | Create, run and key-bind bulk commands | MODERATOR |

Moderators only see the tabs they can use.

---

## Commands

`<player>` = player name or selector, `[x]` = optional.

### General

| Command | Level | Description |
|---|---|---|
| `/abysscore` | MODERATOR | Open the staff menu |
| `/abysscore reload` | MODERATOR | Reload the server config, permissions, titles and bulk commands from disk |
| `/abysscore chat` | ADMIN | Mute or unmute your own chat (see [Chat lock](#chat-lock)) |
| `/abysscore permission grant <player> <level>` | OP | Give a player MODERATOR (or PLAYER) |
| `/abysscore permission revoke <player>` | OP | Set a player back to PLAYER |

### Players

| Command | Level | Description |
|---|---|---|
| `/abysscore god [player]` | MODERATOR | Toggle god mode (no damage) |
| `/abysscore blind <player>` | MODERATOR | Black out the player's screen completely |
| `/abysscore unblind <player>` | MODERATOR | Remove it |
| `/abysscore vanish teamvisibility` | ADMIN | Toggle whether vanished staff can see each other |
| `/abysscore vanish show <player>` | ADMIN | Let a player see you while you're vanished |
| `/abysscore vanish hide <player>` | ADMIN | Remove that exception |
| `/abysscore vanish clearexceptions` | ADMIN | Remove all your exceptions |
| `/abysscore vanish status` | ADMIN | Show your vanish state and exceptions |

### Regions

| Command | Level | Description |
|---|---|---|
| `/abysscore region add <name>` | ADMIN | Create a region from your WorldEdit selection |
| `/abysscore region add <name> <from> <to>` | ADMIN | Create a region from two corners |
| `/abysscore region tp <name>` | ADMIN | Teleport to a region |
| `/abysscore region screen` | ADMIN | Open the Region Manager |

### Dimensions

| Command | Level | Description |
|---|---|---|
| `/abysscore dimen create` | ADMIN | Open the dimension creator (name, style: normal / superflat / void, seed) |
| `/abysscore dimen list` | ADMIN | List AbyssCore dimensions and their state |
| `/abysscore dimen tp <dimension>` | ADMIN | Teleport to a dimension's spawn |
| `/abysscore dimen load <dimension>` | ADMIN | Load an unloaded dimension |
| `/abysscore dimen unload <dimension>` | ADMIN | Unload it (players inside are moved out) |
| `/abysscore dimen remove <dimension>` | ADMIN | Delete it |
| `/abysscore dimen settings <dimension> list` | MODERATOR | Show the gamerules of that dimension |
| `/abysscore dimen settings <dimension> set <rule> <value>` | MODERATOR | Change a gamerule **only in that dimension** |

### Bulk commands, binds and help

| Command | Level | Description |
|---|---|---|
| `/abysscore bulk list` | MODERATOR | List saved bulk commands |
| `/abysscore bulk remove <name>` | MODERATOR | Delete one |
| `/abysscore run <name>` | Depends on the bulk command | Run a saved bulk command |
| `/abysscore bind set <slot 1-9> <bulk name or command>` | ADMIN | Put a bulk command or a raw command on a key slot |
| `/abysscore bind clear <slot 1-9>` | ADMIN | Empty a slot |
| `/abysscore bind list` | ADMIN | Show your slots |
| `/abysshelp <reason>` | Everyone | Ask staff for help |
| `/abysshelp cancel` | Everyone | Cancel your request |
| `/abysshelp list` | MODERATOR | List open requests |
| `/abysscore helpaccept <player>` | MODERATOR | Accept a request and teleport to the player |

### Corruption

| Command | Level | Description |
|---|---|---|
| `/abysscore corruption check <player>` | ADMIN | Show a player's corruption |
| `/abysscore corruption set <player> <0-100>` | ADMIN | Set it |
| `/abysscore corruption add <player> <-100..100>` | ADMIN | Raise or lower it |
| `/abysscore corruption reset <player>` | ADMIN | Back to 0 |

### Client commands

These run on your own game, so they work on any server.

| Command | Description |
|---|---|
| `/corruptionhud` | Show or hide the corruption bar |
| `/abyssgui` | Open the GUI toolkit demo screen (staff, for testing the GUI API) |

---

## Regions

A region is a named box (two corners) in one dimension. On its own it does nothing; you give it **tags** that change the rules inside it. Set tags in the Region Manager (`/abysscore region screen`, or the Regions tab).

| Tag | Inside the region |
|---|---|
| `no_build` | Players can't break or place blocks |
| `no_interact` | Players can't right-click blocks (doors, chests, buttons...) |
| `no_fly` | Flying is turned off |
| `no_friendlyfire` | Players can't attack each other |
| `no_hunger` | Hunger and saturation stay full |
| `no_tp` | Ender pearls, chorus fruit and Waystones are blocked |
| `no_mobspawning_hostile` | Hostile mobs don't spawn |
| `no_mobspawning_pacific` | Passive mobs don't spawn |
| `no_entry` | Players can't walk in. Optionally set a **filter tag**: players with that scoreboard tag are still allowed |
| `keep_inventory` | Players who die here keep their items (in the same slots) and their XP, whatever the world's `keepInventory` gamerule is. Curios slots are not covered |

Server OPs ignore every restriction tag.

---

## Bulk commands and key binds

A **bulk command** is a saved list of commands that runs with one name.

1. In the menu, open **Bulk → + Create**, give it a name, a permission (Everyone or OP only) and its commands.
2. Run it with `/abysscore run <name>`, the **Run** button, or a key.

**Key binds:** in the Bulk tab, select a bulk command and click a slot (1–9) to bind it. Then assign keys to the slots in **Options → Controls → Key Binds → AbyssCore** (they start unbound). Pressing the key runs the command.

Commands always run with the permissions of the player running them, so binding an OP command to a non-OP player's key does not give them OP.

---

## Help requests

Players type `/abysshelp <reason>`. Online staff are notified, and the request shows up in the menu's **Help** tab. Accepting it teleports you to the player.

If a Discord webhook is set in the server config, each request is also posted to that Discord channel.

---

## Chat lock

When `chat_lock_enabled` is `true` in the server config, players below MODERATOR can't send chat messages or use `/say` and `/me`.

Staff can still talk. An ADMIN who wants to stay quiet can mute themselves with `/abysscore chat` (resets when the server restarts).

---

## Corruption

A 0–100 value per player, shown as a purple bar to the left of the hotbar (only while it's above 0). It is kept on death and saved with the player.

AbyssCore only stores and shows it. Other mods decide what raises it and what it does. For example, AbyssItems' Void Essence slowly raises it and gives Strength and Dodge based on it.

---

## Configuration files

Everything is in `config/abysscore/` on the server.

| File | Contents |
|---|---|
| `abysscore-server.json` | Server settings (below) |
| `abysscore_permissions.json` | Granted permission levels |
| `abysscore_titles.json` | Saved titles |
| `abysscore_bulk_commands.json` | Saved bulk commands |

Regions are saved inside the world, per dimension.

**`abysscore-server.json`** (created with defaults on first start):

```json
{
  "chat_lock_enabled": true,
  "help_webhook_enabled": false,
  "help_webhook_url": ""
}
```

| Setting | Default | Description |
|---|---|---|
| `chat_lock_enabled` | `true` | Block chat for players below MODERATOR |
| `help_webhook_enabled` | `false` | Post `/abysshelp` requests to Discord |
| `help_webhook_url` | `""` | The Discord webhook URL |

Run `/abysscore reload` after editing a file; no restart needed.

---

## For developers: the API

Everything under `com.gugas749.abysscore.api` is meant to be used by other mods. The rule of thumb: **AbyssCore provides the mechanism, your mod decides the numbers.**

### Permissions

```java
if (AbyssPermissionHandler.has(player, AbyssPermissionLevel.MODERATOR)) { ... }

// in a command
.requires(source -> AbyssPermissionHandler.sourceHas(source, AbyssPermissionLevel.MODERATOR))

// in a packet handler: checks AND tells the player if they're missing it
if (!AbyssPermissionHandler.require(player, AbyssPermissionLevel.ADMIN)) return;

// client side, for hiding buttons only (the server must still check)
if (AbyssClientPermission.has(AbyssPermissionLevel.MODERATOR)) { ... }
```

> If you add sub-commands under `/abysscore`, put `.requires(...)` on **your** sub-literal, not on `literal("abysscore")`. Brigadier merges all `abysscore` registrations into one node and keeps only the first requirement.

### Damage

```java
target.hurt(AbyssDamageSources.corruption(level), 5f);                            // plain
AbyssDamage.deal(target, AbyssDamageSources.corruption(level), 5f);                // plain
AbyssDamage.deal(target, source, 5f, MobEffects.POISON, 100, 0);                  // damage + effect (100 ticks, level I)
```

Custom damage types are declared in `AbyssDamageTypes` with `DamageTypeBuilder`.

### Continuous effects

Effects that stay on while a condition is true, refreshed before they run out (no night-vision flicker):

```java
AbyssEffectHandler.registerContinuousEffect(player.getUUID(),
        new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
AbyssEffectHandler.unregisterContinuousEffect(player.getUUID(), sameEffectInstance);
```

Cleanup on logout is automatic.

### Screen overlays

Red, purple or any colour on the screen, tied to an effect, a damage type, or triggered by hand:

```java
// while the player has the effect (call from your mod constructor: runs on both sides)
// MyEffects.BLEEDING is a RegistryObject<MobEffect>; vanilla MobEffects.POISON also works
AbyssScreenOverlay.bindEffect(MyEffects.BLEEDING,
        AbyssOverlaySpec.vignette(0xAA0000, 0.5f).scaleWithAmplifier());

// flash when hit by a damage type
AbyssScreenOverlay.bindDamageType(MyDamageTypes.CURSE,
        AbyssOverlaySpec.tint(0x5A0080, 0.35f).fade(2, 15), 20);

// manually, from the server
AbyssScreenOverlay.show(player, new ResourceLocation("mymod", "flash"), AbyssOverlaySpec.tint(0xFFFFFF, 0.8f), 10);
AbyssScreenOverlay.clear(player, new ResourceLocation("mymod", "flash"));
```

`tint` covers the whole screen, `vignette` only the edges. `.fade(in, out)` is in ticks.

### Corruption

```java
AbyssCorruption.add(target, 5);                  // a skill inflicts 5 corruption
AbyssCorruption.addProgress(player, 86_400);     // call every tick: +1 per 86,400 ticks
int level = AbyssCorruption.get(player);         // 0–100, also works on the client (local player)
AbyssCorruption.set(player, 0);
AbyssCorruption.reset(player);
```

### Synced player data

Server-side data that the client can also read, kept on death:

```java
public static final AbyssSyncedAttachment<Integer> MANA =
        AbyssSyncedAttachment.register("mymod", "mana", 0, Codec.INT);

MANA.set(player, 50);          // saves and sends it to the client
MANA.get(player);              // server
MANA.getClient();              // client: last value the server sent
MANA.setSilently(player, 51);  // no packet, for values that change every tick
MANA.sync(player);             // send it when it matters
```

Values live in the player's Forge persistent NBT, are copied on respawn and synced again on login, respawn and dimension change. Make sure the class holding the field is loaded in your mod constructor (on both sides), otherwise the client ignores the sync packets.

### Packets

Registers a packet without loading client classes on a dedicated server:

```java
public static final AbyssNetworkChannel CHANNEL =
        AbyssNetworkChannel.create(new ResourceLocation("mymod", "main"), "1");

AbyssPacketHandler.registerS2C(CHANNEL, MyPacket.class, MyPacket.CODEC,
        () -> MyClientHandler::handle);   // Supplier: only resolved on the client
AbyssPacketHandler.registerC2S(CHANNEL, MyRequest.class, MyRequest.CODEC,
        MyServerHandler::handle);

CHANNEL.sendToPlayer(serverPlayer, new MyPacket(...));
CHANNEL.sendToServer(new MyRequest(...));
```

Packets are plain records with an `AbyssPacketCodec.of(encoder, decoder)`. Registration order is the packet index, so always add new packets at the end and bump the version string when fields change.

### Commands

Each command group implements `AbyssCommand` (one `register(dispatcher)` method), and one class registers them all:

```java
@Mod.EventBusSubscriber(modid = MyMod.MODID)
public class MyCommands {
    private static final List<AbyssCommand> COMMANDS = List.of(FooCommand::register, BarCommand::register);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        COMMANDS.forEach(cmd -> cmd.register(event.getDispatcher()));
    }
}
```

> `AbyssCommandRegistrar` (extend it and register an instance on `MinecraftForge.EVENT_BUS`) also exists, but the pattern above is the tested one.

### GeckoLib

```java
public class MyItem extends AbyssGeoItem {
    public MyItem() { super(new Item.Properties()); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { ... }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) { ... }
}
```

`AbyssGeoItem` and `AbyssGeoEntity` already handle the animation cache, synced-animatable registration and `getTick`.

### GUI toolkit

`api/gui` has the Abyss-styled screen base (`AbyssPanelScreen` with tabs), widgets (`AbyssButton`, `AbyssToggle`, `AbyssTextField`, `AbyssScrollList`, `AbyssSlider`...), layout helpers and dialogs. Run `/abyssgui` in game to see every component.

---

## License

[BSD 3-Clause License](LICENSE) — © 2026 Gugas749
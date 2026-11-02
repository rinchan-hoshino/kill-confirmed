package dev.rinchan.killconfirmedprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.rinchan.killconfirmed.*;
import dev.rinchan.killconfirmed.api.KillConfirmedApi;
import dev.rinchan.killconfirmed.portable.DropStrategy;
import dev.rinchan.killconfirmed.portable.TemplateDocument;
import dev.rinchan.rinlib.v2.PlayerData;
import dev.rinchan.rinlib.api.FunctionExecution;
import dev.rinchan.rinlib.api.MinecraftData;
import dev.rinchan.rinlib.api.PlayerContext;
import dev.rinchan.rinlib.mixin.command.CommandContextAccessor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Real loader deaths, component resolution, item physics and saved-data regression checks. */
public final class RuntimeProbe {
    private static final PlayerData.Slot PENDING = PlayerData.slot("KillConfirmedPendingDogTag");
    private static int assertions;
    private static long nextId = 700;
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        assertions++;
        System.out.println("KC_ASSERT_PASS " + name);
    }

    public static void run(MinecraftServer server) {
        try {
            injectEnglish();
            check(Component.translatable("item.kill_confirmed.dog_tag.named", "LegacyProbe").getString().equals("LegacyProbe's Dog Tag"), "saved dog tags retain their legacy name translation");
            check(Component.translatable("tooltip.kill_confirmed.level", "37").getString().equals("Experience Level: 37"), "saved dog tags retain their legacy lore translations");
            KillConfirmedApi.registerPlaceholder(ResourceLocation.parse("probe:java"), context -> Optional.of(Component.literal("java-provider")));
            if (Files.exists(Path.of("packaged.expected"))) {
                check(KillConfirmed.class.getResource("KillConfirmed.class").toString().contains(".jar"), "release jar supplies the loaded mod classes");
                check(ProbeEntrypoint.bundledLibrary(), "RinLib loads from the nested dog tag jar");
            }
            check(Files.exists(Path.of("config/kill_confirmed.toml")), "startup creates the TOML config");
            check(dev.rinchan.rinlib.v2.net.PresencePayload.type(KillConfirmed.MOD_ID, KillConfirmed.NETWORK_VERSION)
                    .id().equals(KillConfirmed.id("presence_v1")), "client presence payload retains the released protocol ID");
            legacyPendingData(server);
            contextRestoration(server);
            try (var input = RuntimeProbe.class.getResourceAsStream("/legacy-snapshot-1.0.1.snbt")) {
                var legacy = net.minecraft.nbt.TagParser.parseTag(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                var decoded = DogTagSnapshotCodec.decode(legacy, server.registryAccess()).orElseThrow();
                check(decoded.ownerName().equals("LegacyProbe") && decoded.killer().isPresent(), "released 1.0.1 snapshot remains readable");
            }
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set 3624000");
            JsonArray cases = new JsonArray();
            for (String kind : List.of("zombie", "player", "void", "lava")) cases.add(death(server, kind));
            Files.writeString(Path.of("death-cases.json"), new GsonBuilder().setPrettyPrinting().create().toJson(cases) + "\n");
            customization(server);
            respawnDelivery(server);
            Files.writeString(Path.of("runtime.pass"), "assertions=" + assertions + "\n");
            System.out.println("KC_RUNTIME_PASS " + assertions);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.out.println("KC_RUNTIME_FAIL " + failure);
        } finally {
            server.halt(false);
        }
    }

    private static void injectEnglish() throws Exception {
        var base = Language.getInstance();
        Map<String, String> translations = new java.util.HashMap<>();
        try (var input = KillConfirmed.class.getResourceAsStream("/assets/kill_confirmed/lang/en_us.json")) {
            Language.loadFromJson(input, translations::put);
        }
        Language.inject(new Language() {
            public String getOrDefault(String key, String fallback) { return translations.getOrDefault(key, base.getOrDefault(key, fallback)); }
            public boolean has(String key) { return translations.containsKey(key) || base.has(key); }
            public boolean isDefaultRightToLeft() { return false; }
            public FormattedCharSequence getVisualOrder(FormattedText text) { return base.getVisualOrder(text); }
        });
    }

    private static Optional<CompoundTag> takePending(ServerPlayer player) {
        var value = PENDING.get(player);
        PENDING.set(player, null);
        return value;
    }

    private static void legacyPendingData(MinecraftServer server) {
        var player = ProbePlayers.create(server, "LegacyPendingProbe", ++nextId);
        var expected = new CompoundTag();
        expected.putString("id", "kill_confirmed:dog_tag");
        expected.putInt("count", 1);
        var root = new CompoundTag();
        root.put("KillConfirmedPendingDogTag", expected.copy());
        ProbePlayers.load(player, root);
        check(PENDING.get(player).orElseThrow().equals(expected), "legacy root pending tag is readable through PlayerData slot");
        root.remove("KillConfirmedPendingDogTag");
        check(PENDING.get(player).orElseThrow().equals(expected), "loaded pending data is independent of the old root");
        var copy = PENDING.get(player).orElseThrow();
        copy.putString("id", "minecraft:stone");
        check(PENDING.get(player).orElseThrow().equals(expected), "slot reads return private copies");
        PENDING.set(player, expected);
        expected.putString("id", "minecraft:dirt");
        check(PENDING.get(player).orElseThrow().toString().contains("kill_confirmed:dog_tag"), "slot writes retain a copy");
        var saved = ProbePlayers.save(player);
        check(saved.get("KillConfirmedPendingDogTag").equals(PENDING.get(player).orElseThrow()), "slot saves directly at the legacy root key");
        PENDING.set(player, null);
        check(PENDING.get(player).isEmpty() && !ProbePlayers.save(player).contains("KillConfirmedPendingDogTag"), "clearing a slot removes its saved root key");
    }

    private static void contextRestoration(MinecraftServer server) {
        check(CommandContextAccessor.rinlib$currentContext().get() == null, "initial command context is empty");
        Commands.executeCommandInContext(server.createCommandSourceStack(), outer -> {
            try {
                FunctionExecution.isolated(server, () -> {
                    check(CommandContextAccessor.rinlib$currentContext().get() == null, "callback starts in an isolated context");
                    throw new IllegalArgumentException("expected probe exception");
                });
                throw new AssertionError("exception was swallowed");
            } catch (IllegalArgumentException expected) {
                check(CommandContextAccessor.rinlib$currentContext().get() == outer, "exception restores the outer command context");
            }
        });
        check(CommandContextAccessor.rinlib$currentContext().get() == null, "outer command context closes normally");
    }


    private static ServerPlayer player(MinecraftServer server, String name) {
        var player = ProbePlayers.create(server, name, ++nextId);
        player.setPos(0.5, 81, 0.5);
        player.experienceLevel = 37;
        server.overworld().getChunkAt(player.blockPosition());
        server.overworld().addNewPlayer(player);
        // Let vanilla spawn protection expire before applying real lethal damage.
        for (int tick = 0; tick < 61; tick++) player.tick();
        return player;
    }

    private static List<ItemEntity> drops(MinecraftServer server) {
        return server.overworld().getEntitiesOfClass(ItemEntity.class, new AABB(-128, -1024, -128, 128, 320, 128),
                entity -> entity.getItem().is(KillConfirmed.dogTag()));
    }

    private static void clear(MinecraftServer server) { drops(server).forEach(Entity::discard); }

    private static JsonObject death(MinecraftServer server, String kind) throws Exception {
        clear(server);
        var player = player(server, "DeathProbe");
        var killer = player(server, "KillerProbe");
        var sword = new ItemStack(Items.IRON_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Probe Sword"));
        killer.setItemSlot(EquipmentSlot.MAINHAND, sword);
        var zombie = new Zombie(server.overworld());
        if (kind.equals("void")) player.setPos(0.5, -140, 0.5);
        if (kind.equals("lava")) {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                server.overworld().setBlock(new BlockPos(x, 79, z), Blocks.OBSIDIAN.defaultBlockState(), 3);
                for (int y = 80; y <= 82; y++) server.overworld().setBlock(new BlockPos(x, y, z), Blocks.LAVA.defaultBlockState(), 3);
            }
        }
        var source = switch (kind) {
            case "zombie" -> player.damageSources().mobAttack(zombie);
            case "player" -> player.damageSources().playerAttack(killer);
            case "void" -> player.damageSources().fellOutOfWorld();
            case "lava" -> player.damageSources().lava();
            default -> throw new AssertionError(kind);
        };
        ProbePlayers.kill(player, source);
        check(player.isDeadOrDying(), kind + ": lethal damage reaches the real player death event");
        boolean pendingMode = KillConfirmed.config().dropStrategy() == DropStrategy.ON_RESPAWN_INVENTORY;
        ItemStack stack;
        ItemEntity drop = null;
        if (pendingMode) {
            check(drops(server).isEmpty() && PENDING.get(player).isPresent(), kind + ": only pending delivery is created");
            stack = MinecraftData.of(server.registryAccess()).loadItem(takePending(player).orElseThrow()).orElseThrow();
        } else {
            check(drops(server).size() == 1, kind + ": exactly one tag is dropped");
            drop = drops(server).getFirst();
            check(drop.position().distanceTo(player.position()) < 0.001, kind + ": item is at the death position");
            check(PENDING.get(player).isEmpty(), kind + ": there is no respawn fallback");
            stack = drop.getItem().copy();
        }
        var snapshot = KillConfirmedApi.readSnapshot(stack, server.registryAccess()).orElseThrow();
        check(snapshot.ownerName().equals("DeathProbe") && snapshot.experienceLevel() == 37, kind + ": owner and pre-death level are preserved");
        check(snapshot.killer().isPresent() == (kind.equals("zombie") || kind.equals("player")), kind + ": killer presence is correct");
        ((MutableComponent) snapshot.ownerDisplayName()).append("MUTATED");
        check(!snapshot.ownerDisplayName().getString().contains("MUTATED"), kind + ": public snapshot components are copied");
        var name = stack.get(DataComponents.CUSTOM_NAME);
        var lore = stack.get(DataComponents.LORE).lines();
        check(name.getString().equals("DeathProbe's Dog Tag") && !name.getStyle().isItalic(), kind + ": localized non-italic item name");
        check(name.getStyle().getColor().getValue() == 0xFFAA00, kind + ": item name is gold");
        check(lore.size() == 3 && lore.stream().noneMatch(line -> line.getStyle().isItalic()), kind + ": exactly three non-italic default lore lines");
        check(lore.get(1).getString().equals("0, " + (kind.equals("void") ? "-140" : "81") + ", 0  Overworld"), kind + ": coordinates and dimension render in English");
        check(lore.get(2).getString().equals("Level 37  ·  Day 152"), kind + ": level/day render in English");
        var data = MinecraftData.of(server.registryAccess());
        JsonObject result = new JsonObject();
        result.addProperty("death", kind);
        result.add("name", JsonParser.parseString(data.json(name)));
        JsonArray jsonLore = new JsonArray();
        JsonArray plain = new JsonArray();
        for (var line : lore) { jsonLore.add(JsonParser.parseString(data.json(line))); plain.add(line.getString()); }
        result.add("lore", jsonLore);
        result.addProperty("name_en_us", name.getString());
        result.add("lore_en_us", plain);
        int ticks = 0;
        if (drop != null && (kind.equals("void") || kind.equals("lava"))) {
            while (!drop.isRemoved() && ticks < 200) { drop.tick(); ticks++; }
            check(drop.isRemoved(), kind + ": vanilla item physics destroys the unprotected death-position tag");
        }
        result.addProperty("item_ticks_observed", ticks);
        result.addProperty("item_survived", drop == null || !drop.isRemoved());
        result.addProperty("delivery", pendingMode ? "respawn_inventory" : "exact_death_position");
        result.addProperty("respawn_fallback", false);
        Files.writeString(Path.of(kind + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(result) + "\n");
        System.out.println("KC_DEATH " + result);
        clear(server);
        player.discard(); killer.discard(); zombie.discard();
        if (kind.equals("lava")) {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                for (int y = 79; y <= 82; y++) server.overworld().setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        return result;
    }

    // Test-only config swapping exercises alternate settings without exposing a production reload API.
    private static void config(KillConfirmedConfig config) throws Exception {
        var field = KillConfirmed.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(null, config);
    }

    private static void customization(MinecraftServer server) throws Exception {
        var original = KillConfirmed.config();
        var lore = new TemplateDocument(List.of(
                "{\"score\":{\"name\":\"@s\",\"objective\":\"kc_kills\"},\"italic\":false}",
                "{\"nbt\":\"custom\",\"storage\":\"probe:values\",\"italic\":false}",
                "{\"selector\":\"@s\",\"italic\":false}",
                "{\"placeholder\":\"weapon\",\"color\":\"#E2C88C\",\"italic\":false}",
                "{\"placeholder\":\"killer\",\"italic\":false}",
                "{\"text\":\"Hidden label\",\"extra\":[{\"placeholder\":\"missing\"}]}",
                "{", "{\"text\":\"invalid native component\",\"color\":\"rainbow\"}",
                "{\"placeholder\":\"probe:java\",\"italic\":false}"));
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "scoreboard objectives add kc_kills dummy");
        config(new KillConfirmedConfig(DropStrategy.AT_DEATH_POSITION, false, "probe:record", lore));
        // Since 26.1, vanilla pretty-prints non-interpreted NBT strings with quotes.
        var storageValue = server.getServerVersion().startsWith("26.") ? "\"storage-value\"" : "storage-value";
        for (boolean commanded : List.of(false, true)) {
            clear(server);
            var player = player(server, "CustomProbe");
            var killer = player(server, "WeaponProbe");
            var sword = new ItemStack(Items.IRON_SWORD);
            sword.set(DataComponents.CUSTOM_NAME, Component.literal("Probe Sword"));
            killer.setItemSlot(EquipmentSlot.MAINHAND, sword);
            if (commanded) {
                server.getCommands().performPrefixedCommand(PlayerContext.functionSource(player), "function probe:death");
                check(server.getCommandStorage().get(ResourceLocation.parse("probe:checks")).toString().contains("outer_continued:1"), "outer function continues after the synchronous callback");
            } else ProbePlayers.kill(player, player.damageSources().playerAttack(killer));
            var stack = drops(server).getFirst().getItem();
            var lines = stack.get(DataComponents.LORE).lines();
            var text = lines.stream().map(Component::getString).toList();
            System.out.println("KC_CUSTOM_LORE " + text);
            check(text.equals(commanded ? List.of("7", storageValue, "CustomProbe", "java-provider")
                    : List.of("7", storageValue, "CustomProbe", "Probe Sword", "WeaponProbe", "java-provider")), "function values, score/NBT/selector, optional fields and Java providers resolve during " + (commanded ? "function" : "player") + " death");
            check(server.getCommandStorage().get(ResourceLocation.parse("probe:values")).toString().contains("x:0"), "callback runs at the deceased player's position");
            check(!commanded || lines.stream().noneMatch(line -> line.getString().equals("Probe Sword")), "unavailable weapon hides its whole line");
            if (!commanded) check(lines.get(3).getStyle().getColor().getValue() == 0xE2C88C && !lines.get(3).getStyle().isItalic(), "placeholder color and italic styles survive vanilla decoding");
            check(server.getCommandStorage().get(KillConfirmed.id("scratch")).isEmpty(), "no scratch storage is used");
            clear(server); player.discard(); killer.discard();
        }
        config(new KillConfirmedConfig(DropStrategy.AT_DEATH_POSITION, true, "", original.loreTemplate()));
        var victim = player(server, "FilteredProbe");
        var zombie = new Zombie(server.overworld());
        DogTagLifecycle.onDeath(victim, victim.damageSources().mobAttack(zombie));
        DogTagLifecycle.onDeath(victim, victim.damageSources().generic());
        DogTagLifecycle.onDeath(victim, victim.damageSources().playerAttack(victim));
        check(drops(server).isEmpty(), "player-only mode excludes mobs, environment and self-kills");
        var killer = player(server, "PlayerKiller");
        ProbePlayers.kill(victim, victim.damageSources().playerAttack(killer));
        check(drops(server).size() == 1, "player-only mode includes another player");
        clear(server); victim.discard(); killer.discard(); zombie.discard();
        config(original);
    }

    private static ItemStack inventoryDogTag(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.is(KillConfirmed.dogTag())) return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    private static void respawnDelivery(MinecraftServer server) throws Exception {
        var original = KillConfirmed.config();
        config(new KillConfirmedConfig(DropStrategy.ON_RESPAWN_INVENTORY, false, "", original.loreTemplate()));
        var player = player(server, "RespawnProbe");
        ProbePlayers.kill(player, player.damageSources().generic());
        check(PENDING.get(player).isPresent() && drops(server).isEmpty(), "respawn mode stores only pending delivery");
        var pending = PENDING.get(player).orElseThrow();
        DogTagLifecycle.onDeath(player, player.damageSources().generic());
        check(PENDING.get(player).orElseThrow().equals(pending), "repeated death callback cannot replace pending data");
        NbtIo.writeCompressed(ProbePlayers.save(player), Path.of("pending-player.dat"));
        var restored = ProbePlayers.create(server, "RestoredProbe", ++nextId);
        ProbePlayers.load(restored, NbtIo.readCompressed(Path.of("pending-player.dat"), NbtAccounter.unlimitedHeap()));
        check(MinecraftData.of(server.registryAccess()).loadItem(takePending(restored).orElseThrow()).isPresent(), "pending dog tag survives player disk serialization");
        var replacement = server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
        check(PENDING.get(player).isEmpty() && PENDING.get(replacement).isEmpty(), "real respawn transfers and consumes pending data");
        var stack = inventoryDogTag(replacement);
        check(!stack.isEmpty(), "real respawn delivers the dog tag to inventory");
        DogTagLifecycle.onRespawn(replacement);
        int count = 0;
        for (int slot = 0; slot < replacement.getInventory().getContainerSize(); slot++)
            if (replacement.getInventory().getItem(slot).is(KillConfirmed.dogTag())) count++;
        check(count == 1, "repeated respawn callback cannot duplicate tags");
        var data = MinecraftData.of(server.registryAccess());
        PENDING.set(replacement, data.saveItem(stack));
        replacement.getInventory().clearContent();
        for (int slot = 0; slot < 36; slot++) replacement.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        DogTagLifecycle.onRespawn(replacement);
        check(drops(server).size() == 1 && PENDING.get(replacement).isEmpty(), "full inventory drops one tag and clears pending data");
        clear(server); server.getPlayerList().remove(replacement); restored.discard();
        config(original);
    }
}

package dev.rinchan.killconfirmedprobe;
import dev.rinchan.killconfirmed.*;

import dev.rinchan.killconfirmed.api.KillConfirmedApi;
import dev.rinchan.killconfirmed.portable.DropStrategy;
import dev.rinchan.rinlib.api.FunctionExecution;
import dev.rinchan.rinlib.api.MinecraftData;
import dev.rinchan.rinlib.api.PlayerContext;
import dev.rinchan.rinlib.mixin.command.CommandContextAccessor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/** Exercises the actual loader events and Minecraft serialization in a disposable server. */
public final class RuntimeProbe {
    private static int assertions;
    private static long nextId = 700;
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        assertions++;
        System.out.println("KC_ASSERT_PASS " + name);
    }

    public static void run(MinecraftServer server) {
        try {
            KillConfirmedApi.registerPlaceholder(ResourceLocation.parse("probe:java"), context -> Optional.of(Component.literal("java-provider")));
            if (Files.exists(Path.of("packaged.expected"))) {
                check(KillConfirmed.class.getResource("KillConfirmed.class").toString().contains(".jar"), "release jar supplies the loaded mod classes");
                check(ProbeEntrypoint.bundledLibrary(), "RinLib loads from the nested dog tag jar");
            }
            server.getCommandStorage().set(KillConfirmed.id("probe_checks"), new CompoundTag());
            contextRestoration(server);
            try (var input = RuntimeProbe.class.getResourceAsStream("/legacy-snapshot-1.0.1.snbt")) {
                var legacy = net.minecraft.nbt.TagParser.parseTag(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                var decoded = DogTagSnapshotCodec.decode(legacy, server.registryAccess()).orElseThrow();
                check(decoded.ownerName().equals("LegacyProbe") && decoded.killer().isPresent(), "released 1.0.1 snapshot remains readable");
            }
            for (boolean commanded : List.of(false, true)) death(server, commanded);
            invalidProvider(server);
            Files.writeString(Path.of("runtime.pass"), "assertions=" + assertions + "\n");
            System.out.println("KC_RUNTIME_PASS " + assertions);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.out.println("KC_RUNTIME_FAIL " + failure);
        } finally {
            server.halt(false);
        }
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
        player.experienceLevel = 19;
        server.overworld().getChunkAt(player.blockPosition());
        server.overworld().addNewPlayer(player);
        return player;
    }

    private static List<ItemEntity> drops(MinecraftServer server) {
        return server.overworld().getEntitiesOfClass(ItemEntity.class, new AABB(-32, -64, -32, 32, 320, 32),
                entity -> entity.getItem().is(KillConfirmed.dogTag()));
    }

    private static void death(MinecraftServer server, boolean commanded) throws Exception {
        drops(server).forEach(Entity::discard);
        var player = player(server, commanded ? "CommandProbe" : "NaturalProbe");
        var ownerId = player.getUUID();
        var killer = new Zombie(server.overworld());
        if (commanded) {
            server.getCommandStorage().set(KillConfirmed.id("probe_checks"), new CompoundTag());
            server.getCommands().performPrefixedCommand(PlayerContext.functionSource(player), "function probe:death");
            check(server.getCommandStorage().get(KillConfirmed.id("probe_checks")).toString().contains("outer_continued:1"),
                    "outer function resumes after the death callback");
        } else {
            player.setHealth(0);
            player.die(player.damageSources().mobAttack(killer));
        }
        boolean pendingMode = KillConfirmed.config().dropStrategy() == DropStrategy.ON_RESPAWN_INVENTORY;
        ItemStack stack;
        if (pendingMode) {
            var carrier = (PendingDogTagCarrier) player;
            check(carrier.killConfirmed$hasPendingDogTag(), "real death event stores pending dog tag");
            check(drops(server).isEmpty(), "respawn mode does not also drop a dog tag");
            DogTagLifecycle.onDeath(player, player.damageSources().generic());
            check(carrier.killConfirmed$hasPendingDogTag(), "a repeated death callback cannot replace the pending dog tag");
            var saved = ProbePlayers.save(player);
            NbtIo.writeCompressed(saved, Path.of("pending-player.dat"));
            var restored = ProbePlayers.create(server, "RestoredProbe", ++nextId);
            ProbePlayers.load(restored, NbtIo.readCompressed(Path.of("pending-player.dat"), NbtAccounter.unlimitedHeap()));
            var restoredCarrier = (PendingDogTagCarrier) restored;
            check(restoredCarrier.killConfirmed$hasPendingDogTag(), "pending state survives player disk serialization");
            check(MinecraftData.of(server.registryAccess()).loadItem(restoredCarrier.killConfirmed$takePendingDogTag().orElseThrow()).isPresent(),
                    "saved pending item decodes with Minecraft registries");
            check(!restoredCarrier.killConfirmed$hasPendingDogTag(), "taking pending state clears it");
            var replacement = server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
            check(!carrier.killConfirmed$hasPendingDogTag(), "real respawn event transfers pending state from old player");
            check(!((PendingDogTagCarrier) replacement).killConfirmed$hasPendingDogTag(), "real respawn event consumes pending state");
            stack = inventoryDogTag(replacement);
            check(!stack.isEmpty(), "real respawn event delivers the dog tag to inventory");
            DogTagLifecycle.onRespawn(replacement);
            check(countTags(replacement) == 1, "repeated respawn callback does not duplicate dog tags");
            var serialized = MinecraftData.of(server.registryAccess()).saveItem(stack);
            replacement.getInventory().clearContent();
            for (int slot = 0; slot < 36; slot++) replacement.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            ((PendingDogTagCarrier) replacement).killConfirmed$setPendingDogTag(serialized);
            DogTagLifecycle.onRespawn(replacement);
            check(drops(server).size() == 1, "full inventory drops exactly one dog tag");
            check(!((PendingDogTagCarrier) replacement).killConfirmed$hasPendingDogTag(), "overflow delivery clears pending state");
            drops(server).forEach(Entity::discard);
            server.getPlayerList().remove(replacement);
        } else {
            check(drops(server).size() == 1, "real death event drops exactly one dog tag");
            var drop = drops(server).getFirst();
            check(drop.position().distanceTo(player.position()) < 0.001, "dog tag drops at the exact death position");
            check(!((PendingDogTagCarrier) player).killConfirmed$hasPendingDogTag(), "drop mode leaves no pending state");
            stack = drop.getItem().copy();
            drop.discard();
            player.discard();
        }
        var snapshot = KillConfirmedApi.readSnapshot(stack, server.registryAccess()).orElseThrow();
        check(snapshot.ownerId().equals(ownerId) && snapshot.experienceLevel() == 19, "snapshot keeps owner and pre-death level");
        check(snapshot.killer().isPresent() != commanded, "snapshot captures a mob killer and omits a missing killer");
        check(snapshot.deathPosition().getY() == 81, "snapshot keeps death coordinates");
        ((MutableComponent) snapshot.ownerDisplayName()).append("MUTATED");
        check(!snapshot.ownerDisplayName().getString().contains("MUTATED"), "public snapshot components cannot be mutated by providers");
        var lore = stack.get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
        check(lore.contains("nested-function"), "nested datapack callback contributes lore during " + (commanded ? "command" : "natural") + " death");
        check(lore.contains("java-provider"), "Java provider contributes lore");
        check(server.getCommandStorage().get(KillConfirmed.id("scratch")).isEmpty(), "scratch storage clears after callbacks");
        check(KillConfirmedApi.readSnapshot(new ItemStack(Items.STONE), server.registryAccess()).isEmpty(), "other items are not dog tags");
        var encoded = DogTagSnapshotCodec.encode(snapshot, server.registryAccess());
        check(DogTagSnapshotCodec.decode(encoded, server.registryAccess()).orElseThrow().ownerId().equals(ownerId), "schema-one snapshot round trip");
        encoded.putInt("schema", 999);
        check(DogTagSnapshotCodec.decode(encoded, server.registryAccess()).isEmpty(), "unknown snapshot schema is rejected");
        encoded.putInt("schema", 1);
        encoded.putString("killer", "invalid");
        check(DogTagSnapshotCodec.decode(encoded, server.registryAccess()).isEmpty(), "malformed optional killer is rejected");
    }

    private static int countTags(ServerPlayer player) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (player.getInventory().getItem(slot).is(KillConfirmed.dogTag())) total += player.getInventory().getItem(slot).getCount();
        return total;
    }

    private static ItemStack inventoryDogTag(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.is(KillConfirmed.dogTag())) return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    private static void invalidProvider(MinecraftServer server) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "data modify storage kill_confirmed:probe_checks invalid set value 1");
        var player = player(server, "InvalidProbe");
        DogTagLifecycle.onDeath(player, player.damageSources().generic());
        var carrier = (PendingDogTagCarrier) player;
        var first = carrier.killConfirmed$hasPendingDogTag()
                ? MinecraftData.of(server.registryAccess()).loadItem(carrier.killConfirmed$takePendingDogTag().orElseThrow()).orElseThrow()
                : drops(server).getFirst().getItem();
        check(first.get(DataComponents.LORE).lines().stream().noneMatch(line -> line.getString().equals("nested-function")),
                "malformed datapack output is rejected as a whole");
        drops(server).forEach(Entity::discard);
        check(server.getCommandStorage().get(KillConfirmed.id("scratch")).isEmpty(), "failed callback clears scratch storage");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "data remove storage kill_confirmed:probe_checks invalid");
        DogTagLifecycle.onDeath(player, player.damageSources().generic());
        var second = carrier.killConfirmed$hasPendingDogTag()
                ? MinecraftData.of(server.registryAccess()).loadItem(carrier.killConfirmed$takePendingDogTag().orElseThrow()).orElseThrow()
                : drops(server).getFirst().getItem();
        check(second.get(DataComponents.LORE).lines().stream().anyMatch(line -> line.getString().equals("nested-function")),
                "callbacks recover after invalid output");
        drops(server).forEach(Entity::discard);
        player.discard();
    }
}

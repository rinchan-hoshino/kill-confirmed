package dev.rinchan.killconfirmed;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.portable.FunctionOutput;
import dev.rinchan.killconfirmed.portable.FunctionOutputParser;
import dev.rinchan.killconfirmed.portable.PlaceholderKey;
import dev.rinchan.killconfirmed.portable.ReentrantFlag;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.rinchan.rinlib.api.FunctionExecution;
import dev.rinchan.rinlib.api.MinecraftData;
import dev.rinchan.rinlib.api.PlayerContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class DatapackPlaceholderBridge {
    static final ResourceLocation CALLBACK_TAG = KillConfirmed.id("placeholder_providers");
    static final ResourceLocation SCRATCH_STORAGE = KillConfirmed.id("scratch");
    private static final Codec<FunctionOutput> OUTPUT = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(FunctionOutput::id),
            Codec.STRING.fieldOf("component").forGetter(FunctionOutput::componentJson)
    ).apply(instance, FunctionOutput::new));
    private static final ReentrantFlag ACTIVE = new ReentrantFlag();

    private DatapackPlaceholderBridge() {}

    static Map<PlaceholderKey, JsonElement> resolve(ServerPlayer player, DogTagSnapshot snapshot) {
        if (ACTIVE.isSet()) {
            KillConfirmed.LOGGER.error("Refused nested datapack placeholder invocation for {}",
                    PlayerContext.name(player));
            return Map.of();
        }
        var server = ((ServerLevel) player.level()).getServer();
        var storage = server.getCommandStorage();
        try (ReentrantFlag.Scope ignored = ACTIVE.enter()) {
            CompoundTag scratch = new CompoundTag();
            scratch.put("input", encodeInput(snapshot, player));
            scratch.put("output", new ListTag());
            storage.set(SCRATCH_STORAGE, scratch);

            FunctionExecution.executeTag(player, CALLBACK_TAG.toString());

            var outputs = OUTPUT.listOf().fieldOf("output").codec()
                    .parse(NbtOps.INSTANCE, storage.get(SCRATCH_STORAGE)).getOrThrow();

            var parsed = FunctionOutputParser.parse(outputs);
            parsed.errors().forEach(error -> KillConfirmed.LOGGER.error("Datapack placeholder output rejected: {}", error));
            if (!parsed.errors().isEmpty()) return Map.of();

            var data = MinecraftData.of(player.registryAccess());
            Map<PlaceholderKey, JsonElement> validated = new TreeMap<>();
            for (var entry : parsed.values().entrySet()) {
                try {
                    validated.put(entry.getKey(), JsonParser.parseString(
                            data.json(data.component(entry.getValue()))));
                } catch (RuntimeException exception) {
                    KillConfirmed.LOGGER.error("All datapack placeholder output rejected because component {} is invalid: {}",
                            entry.getKey(), exception.getMessage());
                    return Map.of();
                }
            }
            return Collections.unmodifiableMap(validated);
        } catch (RuntimeException exception) {
            KillConfirmed.LOGGER.error("Datapack placeholder callback failed", exception);
            return Map.of();
        } finally {
            storage.set(SCRATCH_STORAGE, new CompoundTag());
        }
    }

    private static CompoundTag encodeInput(DogTagSnapshot snapshot, ServerPlayer player) {
        CompoundTag input = new CompoundTag();
        input.putInt("schema", 1);

        CompoundTag owner = new CompoundTag();
        owner.put("uuid", new IntArrayTag(UUIDUtil.uuidToIntArray(snapshot.ownerId())));
        owner.putString("name", snapshot.ownerName());
        owner.putString("component", DogTagSnapshotCodec.json(snapshot.ownerDisplayName(), player.registryAccess()));
        input.put("owner", owner);

        snapshot.killer().ifPresent(identity -> {
            CompoundTag killer = new CompoundTag();
            killer.put("uuid", new IntArrayTag(UUIDUtil.uuidToIntArray(identity.id())));
            killer.putString("type", identity.typeId().toString());
            killer.putString("component", DogTagSnapshotCodec.json(identity.displayName(), player.registryAccess()));
            input.put("killer", killer);
        });

        input.putString("death_message", DogTagSnapshotCodec.json(snapshot.deathMessage(), player.registryAccess()));
        CompoundTag position = new CompoundTag();
        position.putInt("x", snapshot.deathPosition().getX());
        position.putInt("y", snapshot.deathPosition().getY());
        position.putInt("z", snapshot.deathPosition().getZ());
        input.put("position", position);

        CompoundTag dimension = new CompoundTag();
        dimension.putString("id", snapshot.dimensionId().toString());
        dimension.putString("component", DogTagSnapshotCodec.json(snapshot.dimensionDisplayName(), player.registryAccess()));
        input.put("dimension", dimension);
        input.putInt("experience_level", snapshot.experienceLevel());
        return input;
    }
}

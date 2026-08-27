package dev.rinchan.killconfirmed;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.portable.FunctionOutput;
import dev.rinchan.killconfirmed.portable.FunctionOutputParser;
import dev.rinchan.killconfirmed.portable.PlaceholderKey;
import dev.rinchan.rinlib.state.ReentrantFlag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;

final class DatapackPlaceholderBridge {
    static final Identifier CALLBACK_TAG = KillConfirmed.id("placeholder_providers");
    static final Identifier SCRATCH_STORAGE = KillConfirmed.id("scratch");
    private static final ReentrantFlag ACTIVE = new ReentrantFlag();

    private DatapackPlaceholderBridge() {}

    static Map<PlaceholderKey, JsonElement> resolve(ServerPlayer player, DogTagSnapshot snapshot) {
        if (ACTIVE.isSet()) {
            KillConfirmed.LOGGER.error("Refused nested datapack placeholder invocation for {}",
                    player.getGameProfile().name());
            return Map.of();
        }
        var server = player.level().getServer();
        var storage = server.getCommandStorage();
        storage.set(SCRATCH_STORAGE, new CompoundTag());
        try (ReentrantFlag.Scope ignored = ACTIVE.enter()) {
            CompoundTag scratch = new CompoundTag();
            scratch.put("input", encodeInput(snapshot, player));
            scratch.put("output", new ListTag());
            storage.set(SCRATCH_STORAGE, scratch);

            var source = player.createCommandSourceStack()
                    .withPermission(LevelBasedPermissionSet.GAMEMASTER)
                    .withSuppressedOutput();
            for (var function : server.getFunctions().getTag(CALLBACK_TAG)) {
                server.getFunctions().execute(function, source);
            }

            ListTag raw = storage.get(SCRATCH_STORAGE).getListOrEmpty("output");
            var outputs = new ArrayList<FunctionOutput>(raw.size());
            for (int index = 0; index < raw.size(); index++) {
                CompoundTag entry = raw.getCompoundOrEmpty(index);
                outputs.add(new FunctionOutput(
                        entry.getStringOr("id", ""), entry.getStringOr("component", "")));
            }

            var parsed = FunctionOutputParser.parse(outputs);
            parsed.errors().forEach(error -> KillConfirmed.LOGGER.error("Datapack placeholder output rejected: {}", error));
            if (!parsed.errors().isEmpty()) return Map.of();

            Map<PlaceholderKey, JsonElement> validated = new TreeMap<>();
            for (var entry : parsed.values().entrySet()) {
                try {
                    Component component = DogTagSnapshotCodec.component(entry.getValue(), player.registryAccess());
                    validated.put(entry.getKey(), JsonParser.parseString(
                            DogTagSnapshotCodec.json(component, player.registryAccess())));
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
        owner.store("uuid", UUIDUtil.CODEC, snapshot.ownerId());
        owner.putString("name", snapshot.ownerName());
        owner.putString("component", DogTagSnapshotCodec.json(snapshot.ownerDisplayName(), player.registryAccess()));
        input.put("owner", owner);

        snapshot.killer().ifPresent(identity -> {
            CompoundTag killer = new CompoundTag();
            killer.store("uuid", UUIDUtil.CODEC, identity.id());
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

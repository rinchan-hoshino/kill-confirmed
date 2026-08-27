package dev.rinchan.killconfirmed;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.EntityIdentity;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class DogTagSnapshotCodec {
    static final String ROOT_KEY = "kill_confirmed:snapshot";
    private static final int SNAPSHOT_SCHEMA = 1;

    private DogTagSnapshotCodec() {}

    public static CompoundTag encode(DogTagSnapshot snapshot, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", SNAPSHOT_SCHEMA);
        tag.store("owner_id", UUIDUtil.CODEC, snapshot.ownerId());
        tag.putString("owner_name", snapshot.ownerName());
        tag.putString("owner_display", json(snapshot.ownerDisplayName(), registries));
        snapshot.killer().ifPresent(killer -> {
            CompoundTag killerTag = new CompoundTag();
            killerTag.store("id", UUIDUtil.CODEC, killer.id());
            killerTag.putString("type", killer.typeId().toString());
            killerTag.putString("display", json(killer.displayName(), registries));
            tag.put("killer", killerTag);
        });
        tag.putString("death_message", json(snapshot.deathMessage(), registries));
        tag.putInt("x", snapshot.deathPosition().getX());
        tag.putInt("y", snapshot.deathPosition().getY());
        tag.putInt("z", snapshot.deathPosition().getZ());
        tag.putString("dimension_id", snapshot.dimensionId().toString());
        tag.putString("dimension_display", json(snapshot.dimensionDisplayName(), registries));
        tag.putInt("experience_level", snapshot.experienceLevel());
        return tag;
    }

    public static Optional<DogTagSnapshot> decode(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            Optional<Integer> schema = tag.getInt("schema");
            Optional<UUID> ownerId = tag.read("owner_id", UUIDUtil.CODEC);
            Optional<String> ownerName = tag.getString("owner_name");
            Optional<String> ownerDisplay = tag.getString("owner_display");
            Optional<String> deathMessage = tag.getString("death_message");
            Optional<String> dimensionId = tag.getString("dimension_id");
            Optional<String> dimensionDisplay = tag.getString("dimension_display");
            if (schema.isEmpty() || schema.get() != SNAPSHOT_SCHEMA || ownerId.isEmpty()
                    || ownerName.isEmpty() || ownerDisplay.isEmpty() || deathMessage.isEmpty()
                    || dimensionId.isEmpty() || dimensionDisplay.isEmpty()) {
                return Optional.empty();
            }

            Optional<EntityIdentity> killer = Optional.empty();
            Optional<CompoundTag> killerTag = tag.getCompound("killer");
            if (killerTag.isPresent()) {
                CompoundTag value = killerTag.get();
                Optional<UUID> id = value.read("id", UUIDUtil.CODEC);
                Optional<String> type = value.getString("type");
                Optional<String> display = value.getString("display");
                if (id.isEmpty() || type.isEmpty() || display.isEmpty()) return Optional.empty();
                killer = Optional.of(new EntityIdentity(id.get(), Identifier.parse(type.get()),
                        component(display.get(), registries)));
            }
            return Optional.of(new DogTagSnapshot(
                    ownerId.get(),
                    ownerName.get(),
                    component(ownerDisplay.get(), registries),
                    killer,
                    component(deathMessage.get(), registries),
                    new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0)),
                    Identifier.parse(dimensionId.get()),
                    component(dimensionDisplay.get(), registries),
                    tag.getIntOr("experience_level", 0)));
        } catch (RuntimeException exception) {
            KillConfirmed.LOGGER.error("Rejected malformed dog tag snapshot", exception);
            return Optional.empty();
        }
    }

    public static Optional<DogTagSnapshot> read(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty() || stack.getItem() != KillConfirmed.dogTag()) return Optional.empty();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return Optional.empty();
        CompoundTag root = data.copyTag();
        return root.getCompound(ROOT_KEY).flatMap(tag -> decode(tag, registries));
    }

    static String json(Component component, HolderLookup.Provider registries) {
        JsonElement encoded = ComponentSerialization.CODEC
                .encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), component)
                .getOrThrow();
        return encoded.toString();
    }

    static Component component(String json, HolderLookup.Provider registries) {
        return component(JsonParser.parseString(json), registries);
    }

    static Component component(JsonElement json, HolderLookup.Provider registries) {
        return ComponentSerialization.CODEC
                .parse(registries.createSerializationContext(JsonOps.INSTANCE), json)
                .getOrThrow();
    }
}

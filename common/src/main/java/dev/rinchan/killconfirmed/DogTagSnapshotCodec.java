package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.EntityIdentity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class DogTagSnapshotCodec {
    static final String ROOT_KEY = "kill_confirmed:snapshot";
    private static final int SNAPSHOT_SCHEMA = 1;

    private DogTagSnapshotCodec() {}

    public static CompoundTag encode(DogTagSnapshot snapshot, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", SNAPSHOT_SCHEMA);
        tag.putUUID("owner_id", snapshot.ownerId());
        tag.putString("owner_name", snapshot.ownerName());
        tag.putString("owner_display", json(snapshot.ownerDisplayName(), registries));
        snapshot.killer().ifPresent(killer -> {
            CompoundTag killerTag = new CompoundTag();
            killerTag.putUUID("id", killer.id());
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
            if (!tag.contains("schema", Tag.TAG_INT) || tag.getInt("schema") != SNAPSHOT_SCHEMA
                    || !tag.hasUUID("owner_id") || !tag.contains("owner_name", Tag.TAG_STRING)
                    || !tag.contains("owner_display", Tag.TAG_STRING)
                    || !tag.contains("death_message", Tag.TAG_STRING)
                    || !tag.contains("dimension_id", Tag.TAG_STRING)
                    || !tag.contains("dimension_display", Tag.TAG_STRING)) {
                return Optional.empty();
            }
            Optional<EntityIdentity> killer = Optional.empty();
            if (tag.contains("killer", Tag.TAG_COMPOUND)) {
                CompoundTag value = tag.getCompound("killer");
                if (!value.hasUUID("id") || !value.contains("type", Tag.TAG_STRING)
                        || !value.contains("display", Tag.TAG_STRING)) return Optional.empty();
                killer = Optional.of(new EntityIdentity(value.getUUID("id"), ResourceLocation.parse(value.getString("type")),
                        component(value.getString("display"), registries)));
            }
            return Optional.of(new DogTagSnapshot(
                    tag.getUUID("owner_id"),
                    tag.getString("owner_name"),
                    component(tag.getString("owner_display"), registries),
                    killer,
                    component(tag.getString("death_message"), registries),
                    new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")),
                    ResourceLocation.parse(tag.getString("dimension_id")),
                    component(tag.getString("dimension_display"), registries),
                    tag.getInt("experience_level")));
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
        return root.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? decode(root.getCompound(ROOT_KEY), registries)
                : Optional.empty();
    }

    static String json(Component component, HolderLookup.Provider registries) {
        return Component.Serializer.toJson(component, registries);
    }

    static Component component(String json, HolderLookup.Provider registries) {
        Component component = Component.Serializer.fromJson(json, registries);
        if (component == null) throw new IllegalArgumentException("Component JSON decoded to null");
        return component;
    }
}

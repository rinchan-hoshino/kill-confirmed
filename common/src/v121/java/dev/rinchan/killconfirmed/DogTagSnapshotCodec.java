package dev.rinchan.killconfirmed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.EntityIdentity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Schema-1 NBT codec for DogTagSnapshot (1.21.1), backwards-compatible with v1.2.0 dog tags. */
public final class DogTagSnapshotCodec {
    public static final String ROOT_KEY = "kill_confirmed:snapshot";

    private DogTagSnapshotCodec() {}

    public static Component parseComponent(String json, HolderLookup.Provider registries) {
        Component parsed = Component.Serializer.fromJson(json, registries);
        return parsed != null ? parsed : Component.empty();
    }

    public static String toJson(Component component, HolderLookup.Provider registries) {
        return Component.Serializer.toJson(component, registries);
    }

    private static Codec<DogTagSnapshot> codec(HolderLookup.Provider registries) {
        Codec<Component> text = Codec.STRING.xmap(
                s -> parseComponent(s, registries),
                c -> toJson(c, registries));
        Codec<EntityIdentity> identity = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(EntityIdentity::id),
                ResourceLocation.CODEC.fieldOf("type").forGetter(EntityIdentity::typeId),
                text.fieldOf("display").forGetter(EntityIdentity::displayName)
        ).apply(instance, EntityIdentity::new));
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 1).fieldOf("schema").forGetter(snapshot -> 1),
                UUIDUtil.CODEC.fieldOf("owner_id").forGetter(DogTagSnapshot::ownerId),
                Codec.STRING.fieldOf("owner_name").forGetter(DogTagSnapshot::ownerName),
                text.fieldOf("owner_display").forGetter(DogTagSnapshot::ownerDisplayName),
                identity.optionalFieldOf("killer").forGetter(DogTagSnapshot::killer),
                text.fieldOf("death_message").forGetter(DogTagSnapshot::deathMessage),
                Codec.INT.optionalFieldOf("x", 0).forGetter(snapshot -> snapshot.deathPosition().getX()),
                Codec.INT.optionalFieldOf("y", 0).forGetter(snapshot -> snapshot.deathPosition().getY()),
                Codec.INT.optionalFieldOf("z", 0).forGetter(snapshot -> snapshot.deathPosition().getZ()),
                ResourceLocation.CODEC.fieldOf("dimension_id").forGetter(DogTagSnapshot::dimensionId),
                text.fieldOf("dimension_display").forGetter(DogTagSnapshot::dimensionDisplayName),
                ResourceLocation.CODEC.optionalFieldOf("location_id").forGetter(s -> Optional.of(s.locationId())),
                text.optionalFieldOf("location_display").forGetter(s -> Optional.of(s.locationDisplayName())),
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("experience_level", 0)
                        .forGetter(DogTagSnapshot::experienceLevel)
        ).apply(instance, (schema, id, name, display, killer, message, x, y, z, dimension, dimensionDisplay,
                           locId, locDisplay, level) ->
                new DogTagSnapshot(id, name, display, killer, message, new BlockPos(x, y, z),
                        dimension, dimensionDisplay,
                        locId.orElse(dimension), locDisplay.orElse(dimensionDisplay), level)));
    }

    public static CompoundTag encode(DogTagSnapshot snapshot, HolderLookup.Provider registries) {
        return (CompoundTag) codec(registries).encodeStart(NbtOps.INSTANCE, snapshot).getOrThrow();
    }

    public static Optional<DogTagSnapshot> decode(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            return codec(registries).parse(NbtOps.INSTANCE, tag).result();
        } catch (RuntimeException exception) {
            KillConfirmed.LOGGER.error("Rejected malformed dog tag snapshot", exception);
            return Optional.empty();
        }
    }

    public static Optional<DogTagSnapshot> read(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty() || stack.getItem() != KillConfirmed.dogTag()) return Optional.empty();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return Optional.empty();
        CompoundTag root = customData.copyTag();
        if (!root.contains(ROOT_KEY, CompoundTag.TAG_COMPOUND)) return Optional.empty();
        return decode(root.getCompound(ROOT_KEY), registries);
    }

    /** Fast lightweight reader for owner name without full codec parsing (safe on client/server). */
    public static Optional<String> readOwnerName(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != KillConfirmed.dogTag()) return Optional.empty();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return Optional.empty();
        CompoundTag root = customData.copyTag();
        if (!root.contains(ROOT_KEY, CompoundTag.TAG_COMPOUND)) return Optional.empty();
        String name = root.getCompound(ROOT_KEY).getString("owner_name");
        return name.isBlank() ? Optional.empty() : Optional.of(name);
    }

    public static int readExperienceLevel(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != KillConfirmed.dogTag()) return 0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        CompoundTag root = customData.copyTag();
        if (!root.contains(ROOT_KEY, CompoundTag.TAG_COMPOUND)) return 0;
        return Math.max(0, root.getCompound(ROOT_KEY).getInt("experience_level"));
    }
}

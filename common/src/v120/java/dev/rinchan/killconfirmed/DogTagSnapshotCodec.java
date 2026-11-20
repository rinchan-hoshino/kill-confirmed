package dev.rinchan.killconfirmed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.EntityIdentity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Schema-1 NBT codec for DogTagSnapshot (1.20.1). */
public final class DogTagSnapshotCodec {
    public static final String ROOT_KEY = "kill_confirmed:snapshot";

    private DogTagSnapshotCodec() {}

    public static Component parseComponent(String json) {
        Component parsed = Component.Serializer.fromJson(json);
        return parsed != null ? parsed : Component.empty();
    }

    public static String toJson(Component component) {
        return Component.Serializer.toJson(component);
    }

    private static final Codec<Component> TEXT_CODEC = Codec.STRING.xmap(
            DogTagSnapshotCodec::parseComponent,
            DogTagSnapshotCodec::toJson);

    private static final Codec<EntityIdentity> IDENTITY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(EntityIdentity::id),
            ResourceLocation.CODEC.fieldOf("type").forGetter(EntityIdentity::typeId),
            TEXT_CODEC.fieldOf("display").forGetter(EntityIdentity::displayName)
    ).apply(instance, EntityIdentity::new));

    private static final Codec<DogTagSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 1).fieldOf("schema").forGetter(snapshot -> 1),
            UUIDUtil.CODEC.fieldOf("owner_id").forGetter(DogTagSnapshot::ownerId),
            Codec.STRING.fieldOf("owner_name").forGetter(DogTagSnapshot::ownerName),
            TEXT_CODEC.fieldOf("owner_display").forGetter(DogTagSnapshot::ownerDisplayName),
            IDENTITY_CODEC.optionalFieldOf("killer").forGetter(DogTagSnapshot::killer),
            TEXT_CODEC.fieldOf("death_message").forGetter(DogTagSnapshot::deathMessage),
            Codec.INT.optionalFieldOf("x", 0).forGetter(snapshot -> snapshot.deathPosition().getX()),
            Codec.INT.optionalFieldOf("y", 0).forGetter(snapshot -> snapshot.deathPosition().getY()),
            Codec.INT.optionalFieldOf("z", 0).forGetter(snapshot -> snapshot.deathPosition().getZ()),
            ResourceLocation.CODEC.fieldOf("dimension_id").forGetter(DogTagSnapshot::dimensionId),
            TEXT_CODEC.fieldOf("dimension_display").forGetter(DogTagSnapshot::dimensionDisplayName),
            ResourceLocation.CODEC.optionalFieldOf("location_id").forGetter(s -> Optional.of(s.locationId())),
            TEXT_CODEC.optionalFieldOf("location_display").forGetter(s -> Optional.of(s.locationDisplayName())),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("experience_level", 0)
                    .forGetter(DogTagSnapshot::experienceLevel)
    ).apply(instance, (schema, id, name, display, killer, message, x, y, z, dimension, dimensionDisplay,
                       locId, locDisplay, level) ->
            new DogTagSnapshot(id, name, display, killer, message, new BlockPos(x, y, z),
                    dimension, dimensionDisplay,
                    locId.orElse(dimension), locDisplay.orElse(dimensionDisplay), level)));

    public static CompoundTag encode(DogTagSnapshot snapshot) {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, snapshot).getOrThrow(false, KillConfirmed.LOGGER::error);
    }

    public static Optional<DogTagSnapshot> decode(CompoundTag tag) {
        try {
            return CODEC.parse(NbtOps.INSTANCE, tag).result();
        } catch (RuntimeException exception) {
            KillConfirmed.LOGGER.error("Rejected malformed dog tag snapshot", exception);
            return Optional.empty();
        }
    }

    public static Optional<DogTagSnapshot> read(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != KillConfirmed.dogTag()) return Optional.empty();
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY, CompoundTag.TAG_COMPOUND)) return Optional.empty();
        return decode(root.getCompound(ROOT_KEY));
    }

    public static Optional<String> readOwnerName(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != KillConfirmed.dogTag()) return Optional.empty();
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY, CompoundTag.TAG_COMPOUND)) return Optional.empty();
        String name = root.getCompound(ROOT_KEY).getString("owner_name");
        return name.isBlank() ? Optional.empty() : Optional.of(name);
    }

    public static int readExperienceLevel(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != KillConfirmed.dogTag()) return 0;
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY, CompoundTag.TAG_COMPOUND)) return 0;
        return Math.max(0, root.getCompound(ROOT_KEY).getInt("experience_level"));
    }
}

package dev.rinchan.killconfirmed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.EntityIdentity;
import dev.rinchan.rinlib.minecraft.MinecraftData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** The schema-one NBT layout is shared by every supported Minecraft version. */
public final class DogTagSnapshotCodec {
    static final String ROOT_KEY = "kill_confirmed:snapshot";

    private DogTagSnapshotCodec() {}

    private static Codec<DogTagSnapshot> codec(HolderLookup.Provider registries) {
        var data = MinecraftData.of(registries);
        Codec<Component> text = Codec.STRING.xmap(data::component, data::json);
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
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("experience_level", 0)
                        .forGetter(DogTagSnapshot::experienceLevel)
        ).apply(instance, (schema, id, name, display, killer, message, x, y, z, dimension, dimensionDisplay, level) ->
                new DogTagSnapshot(id, name, display, killer, message, new BlockPos(x, y, z),
                        dimension, dimensionDisplay, level)));
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
        return CompoundTag.CODEC.fieldOf(ROOT_KEY).codec()
                .parse(NbtOps.INSTANCE, MinecraftData.of(registries).customData(stack)).result()
                .flatMap(tag -> decode(tag, registries));
    }

    static String json(Component component, HolderLookup.Provider registries) {
        return MinecraftData.of(registries).json(component);
    }
}

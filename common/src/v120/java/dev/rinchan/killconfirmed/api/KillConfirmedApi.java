package dev.rinchan.killconfirmed.api;

import dev.rinchan.killconfirmed.DogTagSnapshotCodec;
import dev.rinchan.killconfirmed.PlaceholderResolver;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Public integration surface for Kill Confirmed (1.20.1). */
public final class KillConfirmedApi {
    private KillConfirmedApi() {}

    public static void registerPlaceholder(ResourceLocation id, PlaceholderProvider provider) {
        PlaceholderResolver.register(id, Objects.requireNonNull(provider, "provider"));
    }

    public static void registerPlaceholder(String id, PlaceholderProvider provider) {
        registerPlaceholder(new ResourceLocation(Objects.requireNonNull(id, "id")), provider);
    }

    public static Optional<DogTagSnapshot> readSnapshot(ItemStack stack) {
        return DogTagSnapshotCodec.read(stack);
    }
}

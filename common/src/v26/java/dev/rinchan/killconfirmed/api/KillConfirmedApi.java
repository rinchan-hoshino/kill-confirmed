package dev.rinchan.killconfirmed.api;

import dev.rinchan.killconfirmed.DogTagSnapshotCodec;
import dev.rinchan.killconfirmed.PlaceholderResolver;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Public integration surface for Kill Confirmed (26.x). */
public final class KillConfirmedApi {
    private KillConfirmedApi() {}

    public static void registerPlaceholder(Identifier id, PlaceholderProvider provider) {
        PlaceholderResolver.register(id, Objects.requireNonNull(provider, "provider"));
    }

    public static void registerPlaceholder(String id, PlaceholderProvider provider) {
        registerPlaceholder(Identifier.parse(Objects.requireNonNull(id, "id")), provider);
    }

    public static Optional<DogTagSnapshot> readSnapshot(ItemStack stack, HolderLookup.Provider registries) {
        return DogTagSnapshotCodec.read(stack, registries);
    }
}

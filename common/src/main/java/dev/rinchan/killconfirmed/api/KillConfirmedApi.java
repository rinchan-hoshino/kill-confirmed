package dev.rinchan.killconfirmed.api;

import dev.rinchan.killconfirmed.DogTagSnapshotCodec;
import dev.rinchan.killconfirmed.PlaceholderResolver;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Public integration surface for Kill Confirmed. */
public final class KillConfirmedApi {
    private KillConfirmedApi() {}

    /** Registers one custom namespaced placeholder. Built-in and duplicate keys are rejected. */
    public static void registerPlaceholder(Identifier id, PlaceholderProvider provider) {
        PlaceholderResolver.register(id, Objects.requireNonNull(provider, "provider"));
    }

    /** Reads the structured snapshot from a dog tag, or returns empty for another/malformed item. */
    public static Optional<DogTagSnapshot> readSnapshot(ItemStack stack, HolderLookup.Provider registries) {
        return DogTagSnapshotCodec.read(stack, registries);
    }
}

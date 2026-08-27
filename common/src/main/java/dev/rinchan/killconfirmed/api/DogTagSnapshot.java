package dev.rinchan.killconfirmed.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Immutable public death data stored on every dog tag. */
public record DogTagSnapshot(
        UUID ownerId,
        String ownerName,
        Component ownerDisplayName,
        Optional<EntityIdentity> killer,
        Component deathMessage,
        BlockPos deathPosition,
        ResourceLocation dimensionId,
        Component dimensionDisplayName,
        int experienceLevel) {
    public DogTagSnapshot {
        Objects.requireNonNull(ownerId, "ownerId");
        if (Objects.requireNonNull(ownerName, "ownerName").isBlank()) {
            throw new IllegalArgumentException("ownerName must not be blank");
        }
        ownerDisplayName = Objects.requireNonNull(ownerDisplayName, "ownerDisplayName").copy();
        killer = Objects.requireNonNull(killer, "killer");
        deathMessage = Objects.requireNonNull(deathMessage, "deathMessage").copy();
        deathPosition = Objects.requireNonNull(deathPosition, "deathPosition").immutable();
        Objects.requireNonNull(dimensionId, "dimensionId");
        dimensionDisplayName = Objects.requireNonNull(dimensionDisplayName, "dimensionDisplayName").copy();
        if (experienceLevel < 0) throw new IllegalArgumentException("experienceLevel must not be negative");
    }
}

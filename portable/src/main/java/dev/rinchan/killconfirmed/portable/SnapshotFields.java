package dev.rinchan.killconfirmed.portable;

import com.google.gson.JsonElement;
import java.util.Objects;
import java.util.Optional;

public record SnapshotFields(
        JsonElement owner,
        Optional<JsonElement> killer,
        JsonElement deathMessage,
        int x,
        int y,
        int z,
        String dimensionId,
        JsonElement dimensionDisplay,
        int level) {
    public SnapshotFields {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(deathMessage, "deathMessage");
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(dimensionDisplay, "dimensionDisplay");
        if (level < 0) throw new IllegalArgumentException("level must not be negative");
    }
}

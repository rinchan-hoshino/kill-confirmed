package dev.rinchan.killconfirmed.portable;

import com.google.gson.JsonElement;
import java.util.Objects;
import java.util.Optional;

public record SnapshotFields(
        JsonElement owner,
        Optional<JsonElement> killer,
        Optional<JsonElement> weapon,
        JsonElement deathMessage,
        JsonElement locationDisplay,
        int x,
        int y,
        int z,
        String dimensionId,
        JsonElement dimensionDisplay,
        int level,
        long day) {
    public SnapshotFields {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(weapon, "weapon");
        Objects.requireNonNull(deathMessage, "deathMessage");
        Objects.requireNonNull(locationDisplay, "locationDisplay");
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(dimensionDisplay, "dimensionDisplay");
        if (level < 0) throw new IllegalArgumentException("level must not be negative");
    }

    /** Backwards-compatible constructor using dimensionDisplay as locationDisplay. */
    public SnapshotFields(
            JsonElement owner,
            Optional<JsonElement> killer,
            Optional<JsonElement> weapon,
            JsonElement deathMessage,
            int x,
            int y,
            int z,
            String dimensionId,
            JsonElement dimensionDisplay,
            int level,
            long day) {
        this(owner, killer, weapon, deathMessage, dimensionDisplay, x, y, z, dimensionId, dimensionDisplay, level, day);
    }
}

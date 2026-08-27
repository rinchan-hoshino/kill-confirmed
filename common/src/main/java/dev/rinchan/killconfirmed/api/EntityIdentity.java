package dev.rinchan.killconfirmed.api;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** A stable entity identity captured at the instant of death. */
public record EntityIdentity(UUID id, Identifier typeId, Component displayName) {
    public EntityIdentity {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(typeId, "typeId");
        displayName = Objects.requireNonNull(displayName, "displayName").copy();
    }
}

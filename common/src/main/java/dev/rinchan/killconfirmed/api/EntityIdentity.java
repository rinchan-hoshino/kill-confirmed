package dev.rinchan.killconfirmed.api;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A stable entity identity captured at the instant of death. */
public record EntityIdentity(UUID id, ResourceLocation typeId, Component displayName) {
    public EntityIdentity {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(typeId, "typeId");
        displayName = Objects.requireNonNull(displayName, "displayName").copy();
    }
}

package dev.rinchan.killconfirmed.api;

import java.util.Optional;
import net.minecraft.network.chat.Component;

/** Resolves a custom placeholder value for a dog tag being minted (26.x). */
@FunctionalInterface
public interface PlaceholderProvider {
    Optional<Component> resolve(PlaceholderContext context);
}

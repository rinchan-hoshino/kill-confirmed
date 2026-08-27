package dev.rinchan.killconfirmed.api;

import java.util.Optional;
import net.minecraft.network.chat.Component;

@FunctionalInterface
public interface PlaceholderProvider {
    Optional<Component> resolve(PlaceholderContext context) throws Exception;
}

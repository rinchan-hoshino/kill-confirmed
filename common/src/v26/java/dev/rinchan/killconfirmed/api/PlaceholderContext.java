package dev.rinchan.killconfirmed.api;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Context handed to custom placeholder providers when a dog tag is minted (26.x). */
public record PlaceholderContext(DogTagSnapshot snapshot, ServerPlayer player) {
    public PlaceholderContext {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(player, "player");
    }
}

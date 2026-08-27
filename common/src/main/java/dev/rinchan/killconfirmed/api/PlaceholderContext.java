package dev.rinchan.killconfirmed.api;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Context passed synchronously to Java placeholder providers on the server thread. */
public record PlaceholderContext(DogTagSnapshot snapshot, ServerPlayer player) {
    public PlaceholderContext {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(player, "player");
    }
}

package dev.rinchan.killconfirmed.rinlib.api;

import dev.rinchan.killconfirmed.rinlib.mixin.CommandContextAccessor;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

/** Runs function callbacks to completion, including calls made from another function. */
public final class FunctionExecution {
    private FunctionExecution() {}

    /** Executes a function tag synchronously with the player's permission-level-two source. */
    public static int executeTag(ServerPlayer player, String tagId) {
        var server = ((ServerLevel) player.level()).getServer();
        var id = ResourceLocation.parse(tagId);
        return isolated(server, () -> {
            int count = 0;
            var source = PlayerContext.functionSource(player);
            for (var function : server.getFunctions().getTag(id)) {
                server.getFunctions().execute(function, source);
                count++;
            }
            return count;
        });
    }

    public static <T> T isolated(MinecraftServer server, Supplier<T> action) {
        if (!server.isSameThread()) {
            throw new IllegalStateException("Function callbacks must run on the server thread");
        }
        var contexts = CommandContextAccessor.killConfirmed$currentContext();
        var previous = contexts.get();
        contexts.remove();
        try {
            return action.get();
        } finally {
            if (previous == null) contexts.remove();
            else contexts.set(previous);
        }
    }
}

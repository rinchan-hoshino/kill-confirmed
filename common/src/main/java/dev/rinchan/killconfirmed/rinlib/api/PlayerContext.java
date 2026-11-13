package dev.rinchan.killconfirmed.rinlib.api;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/** Player identity and operator-level function sources across Minecraft versions. */
public final class PlayerContext {
    private PlayerContext() {}

    public static String name(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    public static CommandSourceStack functionSource(ServerPlayer player) {
        return player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
    }
}

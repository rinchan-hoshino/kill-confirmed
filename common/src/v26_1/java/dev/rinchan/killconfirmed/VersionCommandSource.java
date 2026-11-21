package dev.rinchan.killconfirmed;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;

final class VersionCommandSource {
    private VersionCommandSource() {}

    static CommandSourceStack create(ServerPlayer player, ServerLevel serverLevel) {
        return new CommandSourceStack(
                CommandSource.NULL,
                player.position(),
                player.getRotationVector(),
                serverLevel,
                LevelBasedPermissionSet.GAMEMASTER,
                player.getScoreboardName(),
                player.getDisplayName(),
                serverLevel.getServer(),
                player).withSuppressedOutput();
    }
}

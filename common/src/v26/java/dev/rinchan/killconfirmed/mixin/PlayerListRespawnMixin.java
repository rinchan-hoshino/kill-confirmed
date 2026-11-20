package dev.rinchan.killconfirmed.mixin;

import dev.rinchan.killconfirmed.DogTagLifecycle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerListRespawnMixin {
    @Inject(method = "respawn", at = @At("RETURN"))
    private void killConfirmed$onRespawn(
            ServerPlayer player,
            boolean keepInventory,
            Entity.RemovalReason reason,
            CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer respawned = cir.getReturnValue();
        if (respawned != null) {
            DogTagLifecycle.onRespawn(respawned);
        }
    }
}
